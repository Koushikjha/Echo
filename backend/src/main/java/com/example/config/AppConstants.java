package com.example.config;

public final class AppConstants {

    private AppConstants() {}

    public static final long   JWT_EXPIRY_MS                 = 86_400_000L;
    public static final String JWT_HEADER                    = "Authorization";
    public static final String JWT_PREFIX                    = "Bearer ";

    public static final long   SESSION_TTL_SEC               = 86_400L;
    public static final long   RISK_SCORE_CACHE_TTL_SEC      = 3_600L;
    public static final long   TRIGGER_CACHE_TTL_SEC         = 900L;


    public static final int    DEFAULT_PAGE_SIZE             = 20;

}