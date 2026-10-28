package com.example.advancednpc.modules.dialogue;

import java.util.UUID;

public class DialogueSession {

    private final UUID         playerId;
    private final UUID         npcId;
    private       DialogueNode currentNode;

    public DialogueSession(UUID playerId, UUID npcId, DialogueNode rootNode) {
        this.playerId    = playerId;
        this.npcId       = npcId;
        this.currentNode = rootNode;
    }

    public UUID         getPlayerId()                    { return playerId; }
    public UUID         getNpcId()                       { return npcId; }
    public DialogueNode getCurrentNode()                 { return currentNode; }
    public void         setCurrentNode(DialogueNode node){ this.currentNode = node; }
}
