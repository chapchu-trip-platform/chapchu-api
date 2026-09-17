package com.pettrip.stamp.controller;

import java.util.List;

public record StampCollectionResponse(
    int acquiredCount, int totalCount, List<StampResponse> stamps) {}
