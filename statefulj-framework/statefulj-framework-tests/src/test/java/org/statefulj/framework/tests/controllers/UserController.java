package org.statefulj.framework.tests.controllers;

import static org.statefulj.framework.tests.model.User.FIVE_STATE;
import static org.statefulj.framework.tests.model.User.FOUR_STATE;
import static org.statefulj.framework.tests.model.User.ONE_STATE;
import static org.statefulj.framework.tests.model.User.SEVEN_STATE;
import static org.statefulj.framework.tests.model.User.SIX_STATE;
import static org.statefulj.framework.tests.model.User.THREE_STATE;
import static org.statefulj.framework.tests.model.User.TWO_STATE;

import javax.annotation.Resource;

import org.springframework.web.bind.annotation.ExceptionHandler;
import org.statefulj.framework.core.annotations.StatefulController;
import org.statefulj.framework.core.annotations.Transition;
import org.statefulj.framework.tests.dao.UserRepository;
import org.statefulj.framework.tests.model.User;

@StatefulController(clazz = User.class, startState = ONE_STATE, blockingStates = { SIX_STATE }, noops = { @Transition(event = "springmvc:/{id}/four", to = FOUR_STATE),
        @Transition(event = "five", to = FIVE_STATE), @Transition(event = "camel:six", to = SIX_STATE), @Transition(event = "unblock", to = SEVEN_STATE), })
public class UserController {

    @Resource
    UserRepository userRepository;

    @Transition(from = ONE_STATE, event = "springmvc:get:/first", to = TWO_STATE)
    public User oneToTwo(final User user, final String event) {
        userRepository.save(user);
        return user;
    }

    @Transition(from = TWO_STATE, event = "springmvc:post:/{id}/second", to = THREE_STATE)
    public User twoToThree(final User user, final String event) {
        return user;
    }

    @Transition(from = THREE_STATE, event = "springmvc:post:/{id}/second")
    public User threeToThree(final User user, final String event) {
        return user;
    }

    @Transition(event = "springmvc:/{id}/any")
    public User any(final User user, final String event) {
        return user;
    }

    @Transition(event = "jersey:/{id}/one")
    public User jerseyOne(final User user, final String event) {
        return user;
    }

    @Transition(event = "camel:camelOne")
    public void camelOne(final User user, final String event, final Long id) {
    }

    @Transition(event = "camel:camelTwo")
    public void camelTwo(final User user, final String event, final Long id) {
    }

    @ExceptionHandler(Exception.class)
    public String handleError(final Exception e) {
        return "called";
    }
}
