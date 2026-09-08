package com.swornhero.dynamicjoingreetings.message;

import com.swornhero.dynamicjoingreetings.config.DynamicJoinGreetingsConfig;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ThreadLocalRandom;

public final class MessageSelector {
    private static final Map<SelectionKey, SelectionState> STATES =
            new HashMap<>();

    private MessageSelector() {
    }

    public static synchronized DynamicJoinGreetingsConfig.MessageEntry select(
            String poolName,
            DynamicJoinGreetingsConfig.MessagePool pool,
            DynamicJoinGreetingsConfig.SelectionSettings settings,
            UUID playerId
    ) {
        if (pool.messages.isEmpty()) {
            return null;
        }

        UUID historyOwner = settings.rememberPerPlayer
                ? playerId
                : null;

        SelectionKey key = new SelectionKey(poolName, historyOwner);
        SelectionState state = STATES.computeIfAbsent(
                key,
                ignored -> new SelectionState()
        );

        DynamicJoinGreetingsConfig.MessageEntry selected =
                switch (settings.mode) {
                    case RANDOM -> selectRandom(
                            pool.messages,
                            state.lastMessageId,
                            settings.avoidImmediateRepeats
                    );

                    case NO_REPEAT -> selectRandom(
                            pool.messages,
                            state.lastMessageId,
                            true
                    );

                    case SHUFFLE_BAG -> selectFromShuffleBag(
                            pool.messages,
                            state,
                            settings.avoidImmediateRepeats
                    );
                };

        state.lastMessageId = selected.id;
        return selected;
    }

    public static synchronized void clear() {
        STATES.clear();
    }

    private static DynamicJoinGreetingsConfig.MessageEntry selectRandom(
            List<DynamicJoinGreetingsConfig.MessageEntry> messages,
            String lastMessageId,
            boolean avoidImmediateRepeat
    ) {
        List<DynamicJoinGreetingsConfig.MessageEntry> candidates =
                new ArrayList<>(messages);

        if (avoidImmediateRepeat && candidates.size() > 1) {
            candidates.removeIf(message ->
                    message.id.equals(lastMessageId)
            );
        }

        return selectWeighted(candidates);
    }

    private static DynamicJoinGreetingsConfig.MessageEntry
    selectFromShuffleBag(
            List<DynamicJoinGreetingsConfig.MessageEntry> messages,
            SelectionState state,
            boolean avoidImmediateRepeat
    ) {
        if (state.shuffleBag.isEmpty()) {
            refillShuffleBag(
                    state.shuffleBag,
                    messages,
                    state.lastMessageId,
                    avoidImmediateRepeat
            );
        }

        return state.shuffleBag.removeFirst();
    }

    private static void refillShuffleBag(
            Deque<DynamicJoinGreetingsConfig.MessageEntry> shuffleBag,
            List<DynamicJoinGreetingsConfig.MessageEntry> messages,
            String lastMessageId,
            boolean avoidImmediateRepeat
    ) {
        List<DynamicJoinGreetingsConfig.MessageEntry> remaining =
                new ArrayList<>(messages);

        while (!remaining.isEmpty()) {
            DynamicJoinGreetingsConfig.MessageEntry selected =
                    selectWeighted(remaining);

            shuffleBag.addLast(selected);
            remaining.remove(selected);
        }

        if (avoidImmediateRepeat
                && shuffleBag.size() > 1
                && shuffleBag.getFirst().id.equals(lastMessageId)) {
            DynamicJoinGreetingsConfig.MessageEntry first =
                    shuffleBag.removeFirst();

            DynamicJoinGreetingsConfig.MessageEntry second =
                    shuffleBag.removeFirst();

            shuffleBag.addFirst(first);
            shuffleBag.addFirst(second);
        }
    }

    private static DynamicJoinGreetingsConfig.MessageEntry selectWeighted(
            List<DynamicJoinGreetingsConfig.MessageEntry> messages
    ) {
        double totalWeight = messages.stream()
                .mapToDouble(message -> message.weight)
                .sum();

        double target =
                ThreadLocalRandom.current().nextDouble(totalWeight);

        for (DynamicJoinGreetingsConfig.MessageEntry message : messages) {
            target -= message.weight;

            if (target < 0) {
                return message;
            }
        }

        return messages.getLast();
    }

    private record SelectionKey(
            String poolName,
            UUID playerId
    ) {
    }

    private static final class SelectionState {
        private String lastMessageId;
        private final Deque<DynamicJoinGreetingsConfig.MessageEntry>
                shuffleBag = new ArrayDeque<>();
    }
}