package io.hoyatla.animatruc.core.graph;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/**
 * Immutable graph definition containing states and transitions.
 */
public final class AnimationGraph {
    private final String entryStateId;
    private final Map<String, AnimationGraphState> states;
    private final Map<String, List<AnimationGraphTransition>> transitionsByFrom;

    private AnimationGraph(
            String entryStateId,
            Map<String, AnimationGraphState> states,
            Map<String, List<AnimationGraphTransition>> transitionsByFrom) {
        this.entryStateId = entryStateId;
        this.states = states;
        this.transitionsByFrom = transitionsByFrom;
    }

    public String entryStateId() {
        return this.entryStateId;
    }

    public AnimationGraphState state(String id) {
        return this.states.get(id);
    }

    public List<AnimationGraphTransition> transitionsFrom(String stateId) {
        List<AnimationGraphTransition> transitions = this.transitionsByFrom.get(stateId);

        if (transitions == null)
            return Collections.emptyList();

        return transitions;
    }

    public static Builder builder(String entryStateId) {
        return new Builder(entryStateId);
    }

    public static final class Builder {
        private final String entryStateId;
        private final Map<String, AnimationGraphState> states = new HashMap<>();
        private final List<AnimationGraphTransition> transitions = new ArrayList<>();

        private Builder(String entryStateId) {
            this.entryStateId = Objects.requireNonNull(entryStateId, "entryStateId");
        }

        public Builder state(AnimationGraphState state) {
            this.states.put(state.id(), state);
            return this;
        }

        public Builder transition(AnimationGraphTransition transition) {
            this.transitions.add(transition);
            return this;
        }

        public AnimationGraph build() {
            if (!this.states.containsKey(this.entryStateId))
                throw new IllegalStateException("AnimationGraph entry state '" + this.entryStateId + "' is not defined");

            for (AnimationGraphTransition transition : this.transitions) {
                if (!AnimationGraphTransition.ANY_STATE.equals(transition.from()) && !this.states.containsKey(transition.from()))
                    throw new IllegalStateException("Transition source state '" + transition.from() + "' is not defined");
                if (!this.states.containsKey(transition.to()))
                    throw new IllegalStateException("Transition target state '" + transition.to() + "' is not defined");
            }

            Map<String, List<AnimationGraphTransition>> transitionsByFrom = new HashMap<>();

            for (AnimationGraphTransition transition : this.transitions) {
                transitionsByFrom
                        .computeIfAbsent(transition.from(), unused -> new ArrayList<>())
                        .add(transition);
            }

            for (Map.Entry<String, List<AnimationGraphTransition>> entry : transitionsByFrom.entrySet()) {
                entry.getValue().sort((left, right) -> Integer.compare(right.priority(), left.priority()));
                entry.setValue(Collections.unmodifiableList(entry.getValue()));
            }

            return new AnimationGraph(
                    this.entryStateId,
                    Collections.unmodifiableMap(new HashMap<>(this.states)),
                    Collections.unmodifiableMap(transitionsByFrom)
            );
        }
    }
}
