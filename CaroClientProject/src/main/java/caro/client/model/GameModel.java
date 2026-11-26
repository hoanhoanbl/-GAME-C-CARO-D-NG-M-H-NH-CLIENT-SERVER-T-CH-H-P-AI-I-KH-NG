package caro.client.model;

import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleObjectProperty;
import caro.common.model.Board;
import caro.common.model.Symbol;

public class GameModel {
    public final Board board = new Board();
    public final ObjectProperty<Symbol> currentTurn = new SimpleObjectProperty<>(Symbol.X);
}
