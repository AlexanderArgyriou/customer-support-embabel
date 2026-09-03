package com.embabel.demo.agents;

import com.embabel.agent.api.annotation.*;
import com.embabel.agent.api.common.Ai;
import com.embabel.agent.domain.io.UserInput;

@Agent(name = "HelloAgent", description = "Say hello to someone")
public class HelloAgent {
    record Person(String name) {
    }

    record Greeting(String message) {
    }

    @Action(description = "Derive a person from the input")
    public Person derivePerson(UserInput input, Ai ai) {
        return ai.withDefaultLlm().createObject(
                String.format("derive person's name from the input: %s",
                        input),
                Person.class);
    }

    @Action(description = "Generate a greeting for a person")
    @AchievesGoal(description = "Someone has been greeted")
    public Greeting generateGreeting(Person person, Ai ai) {
        return new Greeting(String.format("Hello %s", person.name()));
    }
}
