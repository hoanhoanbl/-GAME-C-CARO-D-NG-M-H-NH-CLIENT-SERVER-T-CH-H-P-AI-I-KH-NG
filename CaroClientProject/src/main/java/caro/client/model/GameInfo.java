package caro.client.model;

public record GameInfo(String roomId, String mySymbol, String opponentName, String firstSymbol, boolean vsBot) { }
