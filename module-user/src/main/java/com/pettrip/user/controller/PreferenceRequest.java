package com.pettrip.user.controller;

import java.util.List;
import java.util.UUID;

public record PreferenceRequest(
    List<UUID> regionIds, List<UUID> themeIds, List<UUID> transportMethodIds) {}
