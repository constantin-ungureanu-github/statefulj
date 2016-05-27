/***
 *
 * Copyright 2014 Andrew Hall
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 */
package org.statefulj.framework.core;

import java.lang.annotation.Annotation;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.tuple.ImmutablePair;
import org.apache.commons.lang3.tuple.Pair;
import org.reflections.Reflections;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.BeanCreationException;
import org.springframework.beans.factory.annotation.QualifierAnnotationAutowireCandidateResolver;
import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.beans.factory.config.ConfigurableListableBeanFactory;
import org.springframework.beans.factory.config.ConstructorArgumentValues;
import org.springframework.beans.factory.config.DependencyDescriptor;
import org.springframework.beans.factory.config.RuntimeBeanReference;
import org.springframework.beans.factory.support.BeanDefinitionBuilder;
import org.springframework.beans.factory.support.BeanDefinitionRegistry;
import org.springframework.beans.factory.support.BeanDefinitionRegistryPostProcessor;
import org.springframework.beans.factory.support.DefaultListableBeanFactory;
import org.springframework.beans.factory.support.ManagedList;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
import org.springframework.core.MethodParameter;
import org.springframework.data.repository.Repository;
import org.springframework.data.repository.core.support.RepositoryFactoryBeanSupport;
import org.statefulj.common.utils.ReflectionUtils;
import org.statefulj.framework.core.actions.DomainEntityMethodInvocationAction;
import org.statefulj.framework.core.actions.MethodInvocationAction;
import org.statefulj.framework.core.annotations.StatefulController;
import org.statefulj.framework.core.annotations.Transition;
import org.statefulj.framework.core.annotations.Transitions;
import org.statefulj.framework.core.fsm.FSM;
import org.statefulj.framework.core.fsm.TransitionImpl;
import org.statefulj.framework.core.model.EndpointBinder;
import org.statefulj.framework.core.model.PersistenceSupportBeanFactory;
import org.statefulj.framework.core.model.ReferenceFactory;
import org.statefulj.framework.core.model.StatefulFSM;
import org.statefulj.framework.core.model.impl.MemoryPersistenceSupportBeanFactoryImpl;
import org.statefulj.framework.core.model.impl.ReferenceFactoryImpl;
import org.statefulj.framework.core.model.impl.StatefulFSMImpl;
import org.statefulj.fsm.model.impl.StateImpl;

import javassist.CannotCompileException;
import javassist.NotFoundException;

/**
 * StatefulFactory is responsible for inspecting all StatefulControllers and building out the StatefulJ framework. The factory is invoked at post processing of the beans but before the beans are
 * instantiated
 */
public class StatefulFactory implements BeanDefinitionRegistryPostProcessor, ApplicationContextAware {

    private ApplicationContext appContext;

    private static final String DEFAULT_PACKAGE = "org.statefulj";

    private static final Logger logger = LoggerFactory.getLogger(StatefulFactory.class);

    private final Pattern binder = Pattern.compile("(([^:]*):)?(.*)");

    private final Map<Class<?>, Set<String>> entityToControllerMappings = new HashMap<Class<?>, Set<String>>();

    private final MemoryPersistenceSupportBeanFactoryImpl memoryPersistenceFactory = new MemoryPersistenceSupportBeanFactoryImpl();

    private final String[] packages;

    /**
     * The default constructor will build the StatefulJ Framework using the binders and persisters from the "org.stateful" package
     *
     */
    public StatefulFactory() {
        this(StatefulFactory.DEFAULT_PACKAGE);
    }

    /**
     * Will build the StatefulJ Framework using the binders and persisters from the packages specified in the parameter list. This constructor will not scan the "org.stateful" packages unless
     * explicitly provided in the parameter list
     *
     * @param packages
     *            This list of packages to scan for the binders and persisters
     */
    public StatefulFactory(final String... packages) {
        this.packages = packages;
    }

    // Resolver that injects the FSM for a given controller. It is inferred by the ClassType or will use the bean Id specified by the value of the
    // FSM Annotation
    //
    class FSMAnnotationResolver extends QualifierAnnotationAutowireCandidateResolver {

        @Override
        public Object getSuggestedValue(final DependencyDescriptor descriptor) {
            Object suggested = null;
            final Field field = descriptor.getField();
            final MethodParameter methodParameter = descriptor.getMethodParameter();

            boolean isStatefulFSM = false;
            String controllerId = null;
            Type genericFieldType = null;
            String fieldName = null;
            org.statefulj.framework.core.annotations.FSM fsmAnnotation = null;

            // If this is a StatefulFSM, parse out the Annotation and Type information
            //
            if (field != null) {
                if (isStatefulFSM(field)) {
                    fsmAnnotation = field.getAnnotation(org.statefulj.framework.core.annotations.FSM.class);
                    genericFieldType = field.getGenericType();
                    fieldName = field.getName();
                    isStatefulFSM = true;
                }
            } else if (methodParameter != null) {
                if (isStatefulFSM(methodParameter)) {
                    fsmAnnotation = methodParameter.getParameterAnnotation(org.statefulj.framework.core.annotations.FSM.class);
                    genericFieldType = methodParameter.getGenericParameterType();
                    fieldName = methodParameter.getParameterName();
                    isStatefulFSM = true;
                }
            }

            // If this is a StatefulFSM field, then resolve bean reference
            //
            if (isStatefulFSM) {

                // Determine the controllerId - either explicit or derviced
                //
                controllerId = getControllerId(fsmAnnotation);
                if (StringUtils.isEmpty(controllerId)) {

                    // Get the Managed Class
                    //
                    final Class<?> managedClass = getManagedClass(fieldName, genericFieldType);

                    // Fetch the Controller from the mapping
                    //
                    controllerId = deriveControllerId(fieldName, managedClass);
                }

                final ReferenceFactory refFactory = new ReferenceFactoryImpl(controllerId);
                suggested = appContext.getBean(refFactory.getStatefulFSMId());
            }

            return (suggested != null) ? suggested : super.getSuggestedValue(descriptor);
        }

        private String getControllerId(final org.statefulj.framework.core.annotations.FSM fsmAnnotation) {
            final String controllerId = (fsmAnnotation != null) ? fsmAnnotation.value() : null;
            return controllerId;
        }

        /**
         * @param field
         * @return
         */
        private boolean isStatefulFSM(final Field field) {
            return (field != null) && field.getType().isAssignableFrom(StatefulFSM.class);
        }

        /**
         * @param methodParameter
         * @return
         */
        private boolean isStatefulFSM(final MethodParameter methodParameter) {
            return (methodParameter != null) && methodParameter.getParameterType().isAssignableFrom(StatefulFSM.class);
        }

        /**
         * @param fieldName
         * @param managedClass
         * @return
         */
        private String deriveControllerId(final String fieldName, final Class<?> managedClass) {
            String controllerId;
            final Set<String> controllers = entityToControllerMappings.get(managedClass);

            if (controllers == null) {
                throw new RuntimeException("Unable to resolve FSM for field " + fieldName);
            }
            if (controllers.size() > 1) {
                throw new RuntimeException("Ambiguous fsm for " + fieldName);
            }

            controllerId = controllers.iterator().next();
            return controllerId;
        }

        /**
         * @param fieldName
         * @param genericFieldType
         * @return
         */
        private Class<?> getManagedClass(final String fieldName, final Type genericFieldType) {
            Class<?> managedClass = null;

            if (genericFieldType instanceof ParameterizedType) {
                final ParameterizedType aType = (ParameterizedType) genericFieldType;
                final Type[] fieldArgTypes = aType.getActualTypeArguments();
                for (final Type fieldArgType : fieldArgTypes) {
                    managedClass = (Class<?>) fieldArgType;
                    break;
                }
            }

            if (managedClass == null) {
                StatefulFactory.logger.error("Field {} isn't parametrized", fieldName);
                throw new RuntimeException("Field " + fieldName + " isn't paramertized");
            }
            return managedClass;
        }

    }

    public void postProcessBeanFactory(final ConfigurableListableBeanFactory reg) throws BeansException {
        final DefaultListableBeanFactory bf = (DefaultListableBeanFactory) reg;
        bf.setAutowireCandidateResolver(new FSMAnnotationResolver());
    }

    public void setApplicationContext(final ApplicationContext applicationContext) throws BeansException {
        appContext = applicationContext;
    }

    public void postProcessBeanDefinitionRegistry(final BeanDefinitionRegistry reg) throws BeansException {
        StatefulFactory.logger.debug("postProcessBeanDefinitionRegistry : enter");
        try {

            // Reflect over StatefulJ
            //
            final Reflections reflections = new Reflections((Object[]) packages);

            // Load up all Endpoint Binders
            //
            final Map<String, EndpointBinder> binders = new HashMap<String, EndpointBinder>();
            loadEndpointBinders(reflections, binders);

            // Load up all PersistenceSupportBeanFactories
            //
            final Map<Class<?>, PersistenceSupportBeanFactory> persistenceFactories = new HashMap<Class<?>, PersistenceSupportBeanFactory>();
            loadPersistenceSupportBeanFactories(reflections, persistenceFactories);

            // Map Controllers and Entities
            //
            final Map<String, Class<?>> controllerToEntityMapping = new HashMap<String, Class<?>>();
            final Map<Class<?>, String> entityToRepositoryMappings = new HashMap<Class<?>, String>();

            mapControllerAndEntityClasses(reg, controllerToEntityMapping, entityToRepositoryMappings, entityToControllerMappings);

            // Iterate thru all StatefulControllers and build the framework
            //
            for (final Entry<String, Class<?>> entry : controllerToEntityMapping.entrySet()) {
                buildFramework(entry.getKey(), entry.getValue(), reg, entityToRepositoryMappings, binders, persistenceFactories);
            }

        } catch (final Exception e) {
            throw new BeanCreationException("Unable to create bean", e);
        }
        StatefulFactory.logger.debug("postProcessBeanDefinitionRegistry : exit");
    }

    /**
     * Iterate thru all beans and fetch the StatefulControllers
     *
     * @param reg
     * @return
     * @throws ClassNotFoundException
     */
    private void mapControllerAndEntityClasses(final BeanDefinitionRegistry reg, final Map<String, Class<?>> controllerToEntityMapping, final Map<Class<?>, String> entityToRepositoryMapping,
            final Map<Class<?>, Set<String>> entityToControllerMappings) throws ClassNotFoundException {

        // Loop thru the bean registry
        //
        for (final String bfName : reg.getBeanDefinitionNames()) {

            final BeanDefinition bf = reg.getBeanDefinition(bfName);

            if (bf.isAbstract()) {
                StatefulFactory.logger.debug("Skipping abstract bean " + bfName);
                continue;
            }

            final Class<?> clazz = getClassFromBeanDefinition(bf, reg);

            if (clazz == null) {
                StatefulFactory.logger.debug("Unable to resolve class for bean " + bfName);
                continue;
            }

            // If it's a StatefulController, map controller to the entity and the entity to the controller
            //
            if (ReflectionUtils.isAnnotationPresent(clazz, StatefulController.class)) {
                mapEntityWithController(controllerToEntityMapping, entityToControllerMappings, bfName, clazz);
            }

            // Else, if the Bean is a Repository, then map the
            // Entity associated with the Repo to the PersistenceSupport object
            //
            else if (RepositoryFactoryBeanSupport.class.isAssignableFrom(clazz)) {
                mapEntityToRepository(entityToRepositoryMapping, bfName, bf);
            }
        }
    }

    /**
     * @param entityToRepositoryMapping
     * @param bfName
     * @param bf
     * @throws ClassNotFoundException
     */
    private void mapEntityToRepository(final Map<Class<?>, String> entityToRepositoryMapping, final String bfName, final BeanDefinition bf) throws ClassNotFoundException {

        // Determine the Entity Class associated with the Repo
        //
        final String value = (String) bf.getPropertyValues().getPropertyValue("repositoryInterface").getValue();
        final Class<?> repoInterface = Class.forName(value);
        Class<?> entityType = null;
        for (final Type type : repoInterface.getGenericInterfaces()) {
            if (type instanceof ParameterizedType) {
                final ParameterizedType parmType = (ParameterizedType) type;
                if (Repository.class.isAssignableFrom((Class<?>) parmType.getRawType()) && (parmType.getActualTypeArguments() != null) && (parmType.getActualTypeArguments().length > 0)) {
                    entityType = (Class<?>) parmType.getActualTypeArguments()[0];
                    break;
                }
            }
        }

        if (entityType == null) {
            throw new RuntimeException("Unable to determine Entity type for class " + repoInterface.getName());
        }

        // Map Entity to the RepositoryFactoryBeanSupport bean
        //
        StatefulFactory.logger.debug("Mapped \"{}\" to repo \"{}\", beanId=\"{}\"", entityType.getName(), value, bfName);

        entityToRepositoryMapping.put(entityType, bfName);
    }

    /**
     * @param controllerToEntityMapping
     * @param entityToControllerMappings
     * @param bfName
     * @param clazz
     */
    private void mapEntityWithController(final Map<String, Class<?>> controllerToEntityMapping, final Map<Class<?>, Set<String>> entityToControllerMappings, final String bfName,
            final Class<?> clazz) {
        StatefulFactory.logger.debug("Found StatefulController, class = \"{}\", beanName = \"{}\"", clazz.getName(), bfName);

        // Ctrl -> Entity
        //
        controllerToEntityMapping.put(bfName, clazz);

        // Entity -> Ctrls
        //
        final Class<?> managedEntity = ReflectionUtils.getFirstClassAnnotation(clazz, StatefulController.class).clazz();
        Set<String> controllers = entityToControllerMappings.get(managedEntity);
        if (controllers == null) {
            controllers = new HashSet<String>();
            entityToControllerMappings.put(managedEntity, controllers);
        }
        controllers.add(bfName);
    }

    private void buildFramework(final String statefulControllerBeanId, final Class<?> statefulControllerClass, final BeanDefinitionRegistry reg, final Map<Class<?>, String> entityToRepositoryMappings,
            final Map<String, EndpointBinder> binders, final Map<Class<?>, PersistenceSupportBeanFactory> persistenceFactories)
            throws CannotCompileException, IllegalArgumentException, NotFoundException, IllegalAccessException, InvocationTargetException, ClassNotFoundException {

        // Determine the managed class
        //
        final StatefulController scAnnotation = ReflectionUtils.getFirstClassAnnotation(statefulControllerClass, StatefulController.class);
        final Class<?> managedClass = scAnnotation.clazz();

        // Is the the Controller and ManagedClass the same? (DomainEntity)
        //
        final boolean isDomainEntity = managedClass.equals(statefulControllerClass);

        // ReferenceFactory will generate all the necessary bean ids
        //
        final ReferenceFactory referenceFactory = new ReferenceFactoryImpl(statefulControllerBeanId);

        // We need to map Transitions across all Methods
        //
        final Map<String, Map<String, Method>> providersMappings = new HashMap<String, Map<String, Method>>();
        final Map<Transition, Method> transitionMapping = new HashMap<Transition, Method>();
        final Map<Transition, Method> anyMapping = new HashMap<Transition, Method>();
        final Set<String> states = new HashSet<String>();
        final Set<String> blockingStates = new HashSet<String>();

        // Fetch Repo info
        //
        final String repoBeanId = getRepoId(entityToRepositoryMappings, managedClass);

        // Get the Persistence Factory
        //
        PersistenceSupportBeanFactory factory = null;
        BeanDefinition repoBeanDefinitionFactory = null;

        // If we don't have a repo mapped to this entity, fall back to memory persister
        //
        if (repoBeanId == null) {
            StatefulFactory.logger.warn("Unable to find Spring Data Repository for {}, using an in-memory persister", managedClass.getName());
            factory = memoryPersistenceFactory;
        } else {
            repoBeanDefinitionFactory = reg.getBeanDefinition(repoBeanId);
            final Class<?> repoClassName = getClassFromBeanClassName(repoBeanDefinitionFactory);

            // Fetch the PersistenceFactory
            //
            factory = persistenceFactories.get(repoClassName);
        }

        // Map the Events and Transitions for the Controller
        //
        mapEventsTransitionsAndStates(statefulControllerClass, providersMappings, transitionMapping, anyMapping, states, blockingStates);

        // Do we have binders?
        //
        final boolean hasBinders = (providersMappings.size() > 0);

        // Iterate thru the providers - building and registering each Binder
        //
        if (hasBinders) {
            for (final Entry<String, Map<String, Method>> entry : providersMappings.entrySet()) {

                // Fetch the binder
                //
                final EndpointBinder binder = binders.get(entry.getKey());

                // Check if we found the binder
                //
                if (binder == null) {
                    StatefulFactory.logger.error("Unable to locate binder: {}", entry.getKey());
                    throw new RuntimeException("Unable to locate binder: " + entry.getKey());
                }

                // Build out the Binder Class
                //
                final Class<?> binderClass = binder.bindEndpoints(statefulControllerBeanId, statefulControllerClass, factory.getIdType(), isDomainEntity, entry.getValue(), referenceFactory);

                // Add the new Binder Class to the Bean Registry
                //
                registerBinderBean(entry.getKey(), referenceFactory, binderClass, reg);
            }
        }

        // -- Build the FSM infrastructure --

        // Build out a set of States
        //
        final List<RuntimeBeanReference> stateBeans = new ManagedList<RuntimeBeanReference>();
        for (final String state : states) {
            StatefulFactory.logger.debug("Registering state \"{}\"", state);
            final String stateId = registerState(referenceFactory, statefulControllerClass, state, blockingStates.contains(state), reg);
            stateBeans.add(new RuntimeBeanReference(stateId));
        }

        // Build out the Action classes and the Transitions
        //
        final RuntimeBeanReference controllerRef = new RuntimeBeanReference(statefulControllerBeanId);
        int cnt = 1;
        final List<String> transitionIds = new LinkedList<String>();
        for (final Entry<Transition, Method> entry : anyMapping.entrySet()) {
            for (final String state : states) {
                final Transition t = entry.getKey();
                final String from = state;
                final String to = (t.to().equals(Transition.ANY_STATE)) ? state : entry.getKey().to();
                final String transitionId = referenceFactory.getTransitionId(cnt++);
                final boolean reload = t.reload();
                registerActionAndTransition(referenceFactory, statefulControllerClass, from, to, reload, entry.getKey(), entry.getValue(), isDomainEntity, controllerRef, transitionId, reg);
                transitionIds.add(transitionId);
            }
        }
        for (final Entry<Transition, Method> entry : transitionMapping.entrySet()) {
            final Transition t = entry.getKey();
            final boolean reload = t.reload();
            final String transitionId = referenceFactory.getTransitionId(cnt++);
            registerActionAndTransition(referenceFactory, statefulControllerClass, entry.getKey().from(), entry.getKey().to(), reload, entry.getKey(), entry.getValue(), isDomainEntity, controllerRef,
                    transitionId, reg);
            transitionIds.add(transitionId);
        }

        // Build out the Managed Entity Factory Bean
        //
        final String factoryId = registerFactoryBean(referenceFactory, factory, scAnnotation, reg);

        // Build out the Managed Entity Finder Bean if we have endpoint binders; otherwise, it's not needed
        //
        String finderId = null;
        if (hasFinder(scAnnotation, repoBeanId)) {
            finderId = registerFinderBean(referenceFactory, factory, scAnnotation, repoBeanId, reg);
        }

        // Build out the Managed Entity State Persister Bean
        //
        final String persisterId = registerPersisterBean(referenceFactory, factory, scAnnotation, managedClass, repoBeanId, repoBeanDefinitionFactory, stateBeans, reg);

        // Build out the FSM Bean
        //
        final String fsmBeanId = registerFSM(referenceFactory, statefulControllerClass, scAnnotation, persisterId, managedClass, finderId, factory.getIdAnnotationType(), reg);

        // Build out the StatefulFSM Bean
        //
        final String statefulFSMBeanId = registerStatefulFSMBean(referenceFactory, managedClass, fsmBeanId, factoryId, transitionIds, reg);

        // Build out the FSMHarness Bean if we have binders; otherwise, it's not needed
        //
        if (hasBinders) {
            registerFSMHarness(referenceFactory, factory, managedClass, statefulFSMBeanId, factoryId, finderId, repoBeanDefinitionFactory, reg);
        }
    }

    private void mapEventsTransitionsAndStates(final Class<?> statefulControllerClass, final Map<String, Map<String, Method>> providerMappings, final Map<Transition, Method> transitionMapping,
            final Map<Transition, Method> anyMapping, final Set<String> states, final Set<String> blockingStates)
            throws IllegalArgumentException, NotFoundException, IllegalAccessException, InvocationTargetException, CannotCompileException {

        // Walk up the Class hierarchy building out the FSM
        //
        if (statefulControllerClass == null) {
            return;
        } else {
            mapEventsTransitionsAndStates(statefulControllerClass.getSuperclass(), providerMappings, transitionMapping, anyMapping, states, blockingStates);
        }

        StatefulFactory.logger.debug("Mapping events and transitions for {}", statefulControllerClass);

        // Pull StateController Annotation
        //
        final StatefulController ctrlAnnotation = statefulControllerClass.getAnnotation(StatefulController.class);

        if (ctrlAnnotation != null) {
            // Add Start State
            //
            states.add(ctrlAnnotation.startState());

            // Add the list of BlockingStates
            //
            blockingStates.addAll(Arrays.asList(ctrlAnnotation.blockingStates()));

            // Map the NOOP Transitions
            //
            for (final Transition transition : ctrlAnnotation.noops()) {
                mapTransition(transition, null, providerMappings, transitionMapping, anyMapping, states);
            }
        }

        // TODO : As we map the events, we need to make sure that the method signature and return
        // types of all the handlers for the event are the same

        for (final Method method : statefulControllerClass.getDeclaredMethods()) {

            // Map the set of transitions as defined by the Transitions annotation
            //
            final Transitions transitions = method.getAnnotation(Transitions.class);
            if (transitions != null) {
                for (final Transition transition : transitions.value()) {
                    mapTransition(transition, method, providerMappings, transitionMapping, anyMapping, states);
                }
            }

            // Map the Transition annotation
            //
            final Transition transition = method.getAnnotation(Transition.class);
            if (transition != null) {
                mapTransition(transition, method, providerMappings, transitionMapping, anyMapping, states);
            }
        }
    }

    private void mapTransition(final Transition transition, final Method method, final Map<String, Map<String, Method>> providerMappings, final Map<Transition, Method> transitionMapping,
            final Map<Transition, Method> anyMapping, final Set<String> states) {

        StatefulFactory.logger.debug("Mapping {}:{}->{}", transition.from(), transition.event(), transition.to());

        final Pair<String, String> providerEvent = parseEvent(transition.event());
        final String provider = providerEvent.getLeft();
        if (provider != null) {
            Map<String, Method> eventMapping = providerMappings.get(provider);
            if (eventMapping == null) {
                eventMapping = new HashMap<String, Method>();
                providerMappings.put(provider, eventMapping);
            }

            // Add to the event mapping if this the first occurrence of an event, or the method
            // has more parameters than the existing mapping
            //
            final Method existing = eventMapping.get(providerEvent.getRight());
            if ((existing == null) || (method.getParameterTypes().length > existing.getParameterTypes().length)) {
                eventMapping.put(providerEvent.getRight(), method);
            }
        }

        if (!transition.from().equals(Transition.ANY_STATE)) {
            states.add(transition.from());
            transitionMapping.put(transition, method);
        } else {
            anyMapping.put(transition, method);
        }
        if (!transition.to().equals(Transition.ANY_STATE)) {
            states.add(transition.to());
        }
    }

    private Pair<String, String> parseEvent(final String event) {
        final Matcher matcher = binder.matcher(event);
        if (!matcher.matches()) {
            throw new RuntimeException("Unable to parse event=" + event);
        }
        return new ImmutablePair<String, String>(matcher.group(2), matcher.group(3));
    }

    private void registerActionAndTransition(final ReferenceFactory referenceFactory, final Class<?> clazz, final String from, String to, final boolean reload, final Transition transition,
            final Method method, final boolean isDomainEntity, final RuntimeBeanReference controllerRef, final String transitionId, final BeanDefinitionRegistry reg) {

        // Remap to="Any" to to=from
        //
        to = (Transition.ANY_STATE.equals(to)) ? from : to;

        StatefulFactory.logger.debug("Registered: {}({})->{}/{}", from, transition.event(), to, (method == null) ? "noop" : method.getName());

        // Build the Action Bean
        //
        RuntimeBeanReference actionRef = null;
        if (method != null) {
            final String actionId = referenceFactory.getActionId(method);
            if (!reg.isBeanNameInUse(actionId)) {
                registerMethodInvocationAction(referenceFactory, method, isDomainEntity, controllerRef, reg, actionId);
            }
            actionRef = new RuntimeBeanReference(actionId);
        }

        registerTransition(referenceFactory, from, to, reload, transition, transitionId, reg, actionRef);
    }

    /**
     * @param referenceFactory
     * @param from
     * @param to
     * @param reload
     * @param transition
     * @param transitionId
     * @param reg
     * @param actionRef
     */
    private void registerTransition(final ReferenceFactory referenceFactory, final String from, final String to, final boolean reload, final Transition transition, final String transitionId,
            final BeanDefinitionRegistry reg, final RuntimeBeanReference actionRef) {
        // Build the Transition Bean
        //
        final BeanDefinition transitionBean = BeanDefinitionBuilder.genericBeanDefinition(TransitionImpl.class).getBeanDefinition();

        final String fromId = referenceFactory.getStateId(from);
        final String toId = referenceFactory.getStateId(to);
        final Pair<String, String> providerEvent = parseEvent(transition.event());

        final ConstructorArgumentValues args = transitionBean.getConstructorArgumentValues();
        args.addIndexedArgumentValue(0, new RuntimeBeanReference(fromId));
        args.addIndexedArgumentValue(1, new RuntimeBeanReference(toId));
        args.addIndexedArgumentValue(2, providerEvent.getRight());
        args.addIndexedArgumentValue(3, actionRef);
        args.addIndexedArgumentValue(4, (transition.from().equals(Transition.ANY_STATE) && transition.to().equals(Transition.ANY_STATE)));
        args.addIndexedArgumentValue(5, reload);
        reg.registerBeanDefinition(transitionId, transitionBean);
    }

    /**
     * @param referenceFactory
     * @param method
     * @param isDomainEntity
     * @param controllerRef
     * @param reg
     * @param actionId
     */
    private void registerMethodInvocationAction(final ReferenceFactory referenceFactory, final Method method, final boolean isDomainEntity, final RuntimeBeanReference controllerRef,
            final BeanDefinitionRegistry reg, final String actionId) {
        // Choose the type of invocationAction based off of
        // whether the controller is a DomainEntity
        //
        final Class<?> methodInvocationAction = (isDomainEntity) ? DomainEntityMethodInvocationAction.class : MethodInvocationAction.class;

        final BeanDefinition actionBean = BeanDefinitionBuilder.genericBeanDefinition(methodInvocationAction).getBeanDefinition();

        final ConstructorArgumentValues args = actionBean.getConstructorArgumentValues();
        args.addIndexedArgumentValue(0, method.getName());
        args.addIndexedArgumentValue(1, method.getParameterTypes());
        args.addIndexedArgumentValue(2, new RuntimeBeanReference(referenceFactory.getFSMId()));

        if (!isDomainEntity) {
            args.addIndexedArgumentValue(3, controllerRef);
        }

        reg.registerBeanDefinition(actionId, actionBean);
    }

    private String registerState(final ReferenceFactory referenceFactory, final Class<?> statefulControllerClass, final String state, final boolean isBlocking, final BeanDefinitionRegistry reg) {

        final String stateId = referenceFactory.getStateId(state);
        final BeanDefinition stateBean = BeanDefinitionBuilder.genericBeanDefinition(StateImpl.class).getBeanDefinition();

        final ConstructorArgumentValues args = stateBean.getConstructorArgumentValues();
        args.addIndexedArgumentValue(0, state);
        args.addIndexedArgumentValue(1, false);
        args.addIndexedArgumentValue(2, isBlocking);

        reg.registerBeanDefinition(stateId, stateBean);

        return stateId;
    }

    private String registerFSM(final ReferenceFactory referenceFactory, final Class<?> statefulControllerClass, final StatefulController scAnnotation, final String persisterId,
            final Class<?> managedClass, final String finderId, final Class<? extends Annotation> idAnnotationType, final BeanDefinitionRegistry reg) {
        final int retryAttempts = scAnnotation.retryAttempts();
        final int retryInterval = scAnnotation.retryInterval();

        final String fsmBeanId = referenceFactory.getFSMId();
        final BeanDefinition fsmBean = BeanDefinitionBuilder.genericBeanDefinition(FSM.class).getBeanDefinition();
        final ConstructorArgumentValues args = fsmBean.getConstructorArgumentValues();
        args.addIndexedArgumentValue(0, fsmBeanId);
        args.addIndexedArgumentValue(1, new RuntimeBeanReference(persisterId));
        args.addIndexedArgumentValue(2, retryAttempts);
        args.addIndexedArgumentValue(3, retryInterval);
        args.addIndexedArgumentValue(4, managedClass);
        args.addIndexedArgumentValue(5, idAnnotationType);
        args.addIndexedArgumentValue(6, appContext);

        if (finderId != null) {
            args.addIndexedArgumentValue(7, new RuntimeBeanReference(finderId));
        }

        reg.registerBeanDefinition(fsmBeanId, fsmBean);
        return fsmBeanId;
    }

    private String registerStatefulFSMBean(final ReferenceFactory referenceFactory, final Class<?> statefulClass, final String fsmBeanId, final String factoryId, final List<String> transitionIds,
            final BeanDefinitionRegistry reg) {
        final String statefulFSMBeanId = referenceFactory.getStatefulFSMId();
        final BeanDefinition statefulFSMBean = BeanDefinitionBuilder.genericBeanDefinition(StatefulFSMImpl.class).getBeanDefinition();
        final ConstructorArgumentValues args = statefulFSMBean.getConstructorArgumentValues();
        args.addIndexedArgumentValue(0, new RuntimeBeanReference(fsmBeanId));
        args.addIndexedArgumentValue(1, statefulClass);
        args.addIndexedArgumentValue(2, new RuntimeBeanReference(factoryId));
        reg.registerBeanDefinition(statefulFSMBeanId, statefulFSMBean);
        statefulFSMBean.setDependsOn(transitionIds.toArray(new String[] {}));
        return statefulFSMBeanId;
    }

    private String registerBinderBean(final String key, final ReferenceFactory referenceFactory, final Class<?> binderClass, final BeanDefinitionRegistry reg) {
        final BeanDefinition def = BeanDefinitionBuilder.genericBeanDefinition(binderClass).getBeanDefinition();
        final String binderId = referenceFactory.getBinderId(key);
        reg.registerBeanDefinition(binderId, def);
        return binderId;
    }

    private String registerFactoryBean(final ReferenceFactory referenceFactory, final PersistenceSupportBeanFactory persistenceFactory, final StatefulController statefulContollerAnnotation,
            final BeanDefinitionRegistry reg) {

        String factoryId = statefulContollerAnnotation.factoryId();

        if (StringUtils.isEmpty(factoryId)) {
            if (persistenceFactory == null) {
                throw new RuntimeException("PersistenceFactory is undefined and no factory bean was specified in the StatefulController Annotation for " + statefulContollerAnnotation.clazz());
            }
            factoryId = referenceFactory.getFactoryId();
            reg.registerBeanDefinition(factoryId, persistenceFactory.buildFactoryBean(statefulContollerAnnotation.clazz()));
        }

        return factoryId;
    }

    private String registerFinderBean(final ReferenceFactory referenceFactory, final PersistenceSupportBeanFactory persistenceFactory, final StatefulController statefulContollerAnnotation,
            final String repoBeanId, final BeanDefinitionRegistry reg) {

        String finderId = statefulContollerAnnotation.finderId();

        if (StringUtils.isEmpty(finderId)) {
            if (persistenceFactory == null) {
                throw new RuntimeException("PersistenceFactory is undefined and no finder bean was specified in the StatefulController Annotation for " + statefulContollerAnnotation.clazz());
            }
            if (StringUtils.isEmpty(repoBeanId)) {
                throw new RuntimeException("No Repository is defined for " + statefulContollerAnnotation.clazz());
            }
            finderId = referenceFactory.getFinderId();
            reg.registerBeanDefinition(finderId, persistenceFactory.buildFinderBean(repoBeanId));
        }

        return finderId;
    }

    private String registerPersisterBean(final ReferenceFactory referenceFactory, final PersistenceSupportBeanFactory persistenceFactory, final StatefulController statefulContollerAnnotation,
            final Class<?> statefulClass, final String repoBeanId, final BeanDefinition repoBeanDefinitionFactory, final List<RuntimeBeanReference> stateBeans, final BeanDefinitionRegistry reg) {

        String persisterId = statefulContollerAnnotation.persisterId();

        if (StringUtils.isEmpty(persisterId)) {
            if (persistenceFactory == null) {
                throw new RuntimeException("PersistenceFactory is undefined and no persister bean was specified in the StatefulController Annotation for " + statefulContollerAnnotation.clazz());
            }
            final String startStateId = referenceFactory.getStateId(statefulContollerAnnotation.startState());
            persisterId = referenceFactory.getPersisterId();
            reg.registerBeanDefinition(persisterId,
                    persistenceFactory.buildPersisterBean(statefulClass, repoBeanId, repoBeanDefinitionFactory, statefulContollerAnnotation.stateField(), startStateId, stateBeans));
        }

        return persisterId;
    }

    private String registerFSMHarness(final ReferenceFactory referenceFactory, final PersistenceSupportBeanFactory persistenceFactory, final Class<?> statefulClass, final String fsmBeanId,
            final String factoryId, final String finderId, final BeanDefinition repoBeanFactory, final BeanDefinitionRegistry reg) {
        final String fsmHarnessId = referenceFactory.getFSMHarnessId();
        reg.registerBeanDefinition(fsmHarnessId, persistenceFactory.buildFSMHarnessBean(statefulClass, fsmBeanId, factoryId, finderId, repoBeanFactory));
        return fsmHarnessId;
    }

    private String getRepoId(final Map<Class<?>, String> entityToRepositoryMappings, final Class<?> clazz) {
        if (clazz != null) {
            String id = entityToRepositoryMappings.get(clazz);
            if (id != null) {
                return id;
            }
            id = getRepoId(entityToRepositoryMappings, clazz.getSuperclass());
            if (id != null) {
                return id;
            }
            for (final Class<?> interfaze : clazz.getInterfaces()) {
                id = getRepoId(entityToRepositoryMappings, interfaze);
                if (id != null) {
                    return id;
                }
            }
        }
        return null;
    }

    private Class<?> getClassFromBeanDefinition(final BeanDefinition bf, final BeanDefinitionRegistry reg) throws ClassNotFoundException {
        Class<?> clazz = null;

        if (bf.getBeanClassName() == null) {
            clazz = getClassFromFactoryMethod(bf, reg);
        } else {
            clazz = getClassFromBeanClassName(bf);
        }

        if (clazz == null) {
            clazz = getClassFromParentBean(bf, reg);
        }

        return clazz;
    }

    /**
     * @param bf
     * @return
     * @throws ClassNotFoundException
     */
    private Class<?> getClassFromBeanClassName(final BeanDefinition bf) throws ClassNotFoundException {
        return Class.forName(bf.getBeanClassName());
    }

    /**
     * @param bf
     * @param reg
     * @param clazz
     * @return
     * @throws ClassNotFoundException
     */
    private Class<?> getClassFromParentBean(final BeanDefinition bf, final BeanDefinitionRegistry reg) throws ClassNotFoundException {
        Class<?> clazz = null;
        final String parentBeanName = bf.getParentName();
        if (parentBeanName != null) {
            final BeanDefinition parent = reg.getBeanDefinition(parentBeanName);
            if (parent != null) {
                clazz = getClassFromBeanDefinition(parent, reg);
            }
        }
        return clazz;
    }

    /**
     * @param bf
     * @param reg
     * @param clazz
     * @return
     * @throws ClassNotFoundException
     */
    private Class<?> getClassFromFactoryMethod(final BeanDefinition bf, final BeanDefinitionRegistry reg) throws ClassNotFoundException {
        Class<?> clazz = null;
        final String factoryBeanName = bf.getFactoryBeanName();
        if (factoryBeanName != null) {
            final BeanDefinition factory = reg.getBeanDefinition(factoryBeanName);
            if (factory != null) {
                final String factoryClassName = factory.getBeanClassName();
                final Class<?> factoryClass = Class.forName(factoryClassName);
                final List<Method> methods = new LinkedList<Method>();
                methods.addAll(Arrays.asList(factoryClass.getMethods()));
                methods.addAll(Arrays.asList(factoryClass.getDeclaredMethods()));
                for (final Method method : methods) {
                    method.setAccessible(true);
                    if (method.getName().equals(bf.getFactoryMethodName())) {
                        clazz = method.getReturnType();
                        break;
                    }
                }
            }
        }
        return clazz;
    }

    /**
     * @param reflections
     * @throws InstantiationException
     * @throws IllegalAccessException
     */
    private void loadPersistenceSupportBeanFactories(final Reflections reflections, final Map<Class<?>, PersistenceSupportBeanFactory> persistenceFactories)
            throws InstantiationException, IllegalAccessException {
        final Set<Class<? extends PersistenceSupportBeanFactory>> persistenceFactoryTypes = reflections.getSubTypesOf(PersistenceSupportBeanFactory.class);
        for (final Class<?> persistenceFactoryType : persistenceFactoryTypes) {
            if (!Modifier.isAbstract(persistenceFactoryType.getModifiers())) {
                final PersistenceSupportBeanFactory factory = (PersistenceSupportBeanFactory) persistenceFactoryType.newInstance();
                final Class<?> key = factory.getKey();
                if (key != null) {
                    persistenceFactories.put(key, factory);
                }
            }
        }
    }

    /**
     * @return
     * @throws InstantiationException
     * @throws IllegalAccessException
     */
    private void loadEndpointBinders(final Reflections reflections, final Map<String, EndpointBinder> binders) throws InstantiationException, IllegalAccessException {
        final Set<Class<? extends EndpointBinder>> endpointBinders = reflections.getSubTypesOf(EndpointBinder.class);
        for (final Class<?> binderClass : endpointBinders) {
            if (!Modifier.isAbstract(binderClass.getModifiers())) {
                final EndpointBinder binder = (EndpointBinder) binderClass.newInstance();
                binders.put(binder.getKey(), binder);
            }
        }
    }

    /**
     * @param scAnnotation
     * @param repoBeanId
     * @return
     */
    private boolean hasFinder(final StatefulController scAnnotation, final String repoBeanId) {
        return !"".equals(scAnnotation.finderId()) || ((repoBeanId != null) && !repoBeanId.trim().equals(""));
    }

}
