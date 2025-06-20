package org.example.catanboardgameapp;

// All possible outcomes when trying to build something
public enum BuildResult {
    SUCCESS,
    TOO_MANY_ROADS,
    TOO_MANY_SETTLEMENTS,
    TOO_MANY_CITIES,
    NOT_CONNECTED,
    INSUFFICIENT_RESOURCES,
    INVALID_EDGE,
    INVALID_VERTEX,
    UPGRADED_TO_CITY
}