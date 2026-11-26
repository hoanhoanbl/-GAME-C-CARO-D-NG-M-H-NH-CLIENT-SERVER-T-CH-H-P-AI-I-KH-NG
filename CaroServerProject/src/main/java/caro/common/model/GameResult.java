package caro.common.model;

import java.util.List;

/** winner = null nếu hòa. */
public record GameResult(Symbol winner, EndReason reason, List<int[]> winLine) { }
