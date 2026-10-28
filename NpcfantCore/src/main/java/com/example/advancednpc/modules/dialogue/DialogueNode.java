package com.example.advancednpc.modules.dialogue;

import com.example.advancednpc.api.action.INPCAction;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class DialogueNode {

    private final List<String>        messages = new ArrayList<>();
    private final List<DialogueOption> options  = new ArrayList<>();


    public DialogueNode addMessage(String message) {
        this.messages.add(message);
        return this;
    }


    public DialogueNode addOption(DialogueOption option) {
        this.options.add(option);
        return this;
    }

    public List<String>         getMessages() { return Collections.unmodifiableList(messages); }
    public List<DialogueOption> getOptions()  { return Collections.unmodifiableList(options); }


    public static DialogueNode leafNode(String... messages) {
        DialogueNode node = new DialogueNode();
        for (String msg : messages) node.addMessage(msg);
        return node;
    }


    public static DialogueNode of(String... messages) {
        return leafNode(messages);
    }
}
