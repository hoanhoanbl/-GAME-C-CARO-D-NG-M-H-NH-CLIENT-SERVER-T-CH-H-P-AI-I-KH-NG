package caro.client.model;

import javafx.collections.FXCollections;
import javafx.collections.ObservableList;

public class ChatModel {
    public final ObservableList<ChatLine> lines = FXCollections.observableArrayList();
}
