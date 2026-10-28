package com.example.advancednpc.modules.dialogue;

import com.example.advancednpc.api.action.INPCAction;

import java.util.ArrayList;
import java.util.List;

public class DialogueOption {

    private final String           label;
    private final List<INPCAction> actions  = new ArrayList<>();
    private       DialogueNode     nextNode;


    public DialogueOption(String label) {
        this.label = label;
    }


    public DialogueOption addAction(INPCAction action) {
        this.actions.add(action);
        return this;
    }


    public DialogueOption then(DialogueNode next) {
        this.nextNode = next;
        return this;
    }

    public String           getLabel()    { return label; }
    public List<INPCAction> getActions()  { return actions; }
    public DialogueNode     getNextNode() { return nextNode; }
}
