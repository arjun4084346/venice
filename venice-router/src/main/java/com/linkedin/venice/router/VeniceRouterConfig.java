package com.linkedin.venice.router;

import com.linkedin.venice.exceptions.VeniceException;
import com.linkedin.venice.router.cache.CacheEviction;
import com.linkedin.venice.router.cache.CacheType;
import com.linkedin.venice.utils.VeniceProperties;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.concurrent.TimeUnit;
import org.apache.log4j.Logger;

import static com.linkedin.venice.ConfigKeys.*;


/**
 * Configuration for Venice Router.
 */
public class VeniceRouterConfig {
  private static final Logger logger = Logger.getLogger(VeniceRouterConfig.class);

  private String clusterName;
  private String zkConnection;
  private int port;
  private int sslPort;
  private int clientTimeoutMs;
  private int heartbeatTimeoutMs;
  private boolean sslToStorageNodes;
  private long maxReadCapacityCu;
  private int longTailRetryForSingleGetThresholdMs;
  private TreeMap<Integer, Integer> longTailRetryForBatchGetThresholdMs;
  private int maxKeyCountInMultiGetReq;
  private int connectionLimit;
  private int httpClientPoolSize;
  private int maxOutgoingConnPerRoute;
  private int maxOutgoingConn;
  private Map<String, String> clusterToD2Map;
  private boolean stickyRoutingEnabledForSingleGet;
  private boolean stickyRoutingEnabledForMultiGet;
  private double perStorageNodeReadQuotaBuffer;
  private int refreshAttemptsForZkReconnect;
  private long refreshIntervalForZkReconnectInMs;
  private boolean cacheEnabled;
  private long cacheSizeBytes;
  private int cacheConcurrency;
  private CacheType cacheType;
  private CacheEviction cacheEviction;
  private int cacheHashTableSize;
  private double cacheHitRequestThrottleWeight;
  private int routerNettyGracefulShutdownPeriodSeconds;
  private boolean enforceSecureOnly;
  private boolean dnsCacheEnabled;
  private String hostPatternForDnsCache;
  private long dnsCacheRefreshIntervalInMs;

  public VeniceRouterConfig(VeniceProperties props) {
    try {
      checkProperties(props);
      logger.info("Loaded configuration");
    } catch (Exception e) {
      String errorMessage = "Can not load properties.";
      logger.error(errorMessage);
      throw new VeniceException(errorMessage, e);
    }
  }

  private void checkProperties(VeniceProperties props) {
    clusterName = props.getString(CLUSTER_NAME);
    port = props.getInt(LISTENER_PORT);
    sslPort = props.getInt(LISTENER_SSL_PORT);
    zkConnection = props.getString(ZOOKEEPER_ADDRESS);
    clientTimeoutMs = props.getInt(CLIENT_TIMEOUT, 10000); //10s
    heartbeatTimeoutMs = props.getInt(HEARTBEAT_TIMEOUT, 1000); //1s
    sslToStorageNodes = props.getBoolean(SSL_TO_STORAGE_NODES, false); // disable ssl on path to stroage node by default.
    maxReadCapacityCu = props.getLong(MAX_READ_CAPCITY, 100000); //100000 CU
    longTailRetryForSingleGetThresholdMs = props.getInt(ROUTER_LONG_TAIL_RETRY_FOR_SINGLE_GET_THRESHOLD_MS, 15); //15 ms
    longTailRetryForBatchGetThresholdMs = parseRetryThresholdForBatchGet(
        props.getString(ROUTER_LONG_TAIL_RETRY_FOR_BATCH_GET_THRESHOLD_MS, "1-5:15,6-20:30,21-150:50,151-500:100,501-:500"));
    maxKeyCountInMultiGetReq = props.getInt(ROUTER_MAX_KEY_COUNT_IN_MULTIGET_REQ, 500);
    connectionLimit = props.getInt(ROUTER_CONNECTION_LIMIT, 10000);
    httpClientPoolSize = props.getInt(ROUTER_HTTP_CLIENT_POOL_SIZE, 12);
    maxOutgoingConnPerRoute = props.getInt(ROUTER_MAX_OUTGOING_CONNECTION_PER_ROUTE, 120);
    maxOutgoingConn = props.getInt(ROUTER_MAX_OUTGOING_CONNECTION, 1200);
    clusterToD2Map = props.getMap(CLUSTER_TO_D2);
    stickyRoutingEnabledForSingleGet = props.getBoolean(ROUTER_ENABLE_STICKY_ROUTING_FOR_SINGLE_GET, true);
    stickyRoutingEnabledForMultiGet = props.getBoolean(ROUTER_ENABLE_STICKY_ROUTING_FOR_MULTI_GET, true);
    perStorageNodeReadQuotaBuffer = props.getDouble(ROUTER_PER_STORAGE_NODE_READ_QUOTA_BUFFER, 1.0);
    refreshAttemptsForZkReconnect = props.getInt(REFRESH_ATTEMPTS_FOR_ZK_RECONNECT, 3);
    refreshIntervalForZkReconnectInMs =
        props.getLong(REFRESH_INTERVAL_FOR_ZK_RECONNECT_MS, java.util.concurrent.TimeUnit.SECONDS.toMillis(10));
    cacheEnabled = props.getBoolean(ROUTER_CACHE_ENABLED, false);
    cacheSizeBytes = props.getSizeInBytes(ROUTER_CACHE_SIZE_IN_BYTES, 500 * 1024 * 1024l); // 500MB
    cacheConcurrency = props.getInt(ROUTER_CACHE_CONCURRENCY, 16);
    cacheType = CacheType.valueOf(props.getString(ROUTER_CACHE_TYPE, CacheType.OFF_HEAP_CACHE.name()));
    cacheEviction = CacheEviction.valueOf(props.getString(ROUTER_CACHE_EVICTION, CacheEviction.W_TINY_LFU.name()));
    cacheHashTableSize = props.getInt(ROUTER_CACHE_HASH_TABLE_SIZE, 1024 * 1024); // 1M
    /**
     * Make the default value for the throttle weight of cache hit request to be 1, which is same as the regular request.
     * The reason behind this:
     * 1. Easy to reason w/o cache on both customer and Venice side;
     * 2. Hot key problem is still being alleviated since cache hit request is only being counted when calculating
     * store-level quota, not storage-node level quota, which means the hot keys routing to the same router at most
     * could use (total_quota / total_router_number);
     *
     * If it is not working well, we could adjust this config later on.
     */
    cacheHitRequestThrottleWeight = props.getDouble(ROUTER_CACHE_HIT_REQUEST_THROTTLE_WEIGHT, 1);
    routerNettyGracefulShutdownPeriodSeconds = props.getInt(ROUTER_NETTY_GRACEFUL_SHUTDOWN_PERIOD_SECONDS, 30); //30s
    enforceSecureOnly = props.getBoolean(ENFORCE_SECURE_ROUTER, false);

    // This only needs to be enabled in some DC, where slow DNS lookup happens.
    dnsCacheEnabled = props.getBoolean(ROUTER_DNS_CACHE_ENABLED, false);
    hostPatternForDnsCache = props.getString(ROUTE_DNS_CACHE_HOST_PATTERN, ".*prod.linkedin.com");
    dnsCacheRefreshIntervalInMs = props.getLong(ROUTER_DNS_CACHE_REFRESH_INTERVAL_MS, TimeUnit.MINUTES.toMillis(3)); // 3 mins
  }

  public String getClusterName() {
    return clusterName;
  }

  public String getZkConnection() {
    return zkConnection;
  }

  public int getPort() {
    return port;
  }

  public int getSslPort() {
    return sslPort;
  }

  public int getClientTimeoutMs() {
    return clientTimeoutMs;
  }

  public boolean isStickyRoutingEnabledForSingleGet() {
    return stickyRoutingEnabledForSingleGet;
  }

  public boolean isStickyRoutingEnabledForMultiGet() {
    return stickyRoutingEnabledForMultiGet;
  }

  public int getHeartbeatTimeoutMs() {
    return heartbeatTimeoutMs;
  }

  public boolean isSslToStorageNodes() {
    return sslToStorageNodes;
  }

  public long getMaxReadCapacityCu() {
    return maxReadCapacityCu;
  }

  public int getLongTailRetryForSingleGetThresholdMs() {
    return longTailRetryForSingleGetThresholdMs;
  }

  public int getMaxKeyCountInMultiGetReq() {
    return maxKeyCountInMultiGetReq;
  }

  public Map<String, String> getClusterToD2Map() {
    return clusterToD2Map;
  }

  public int getConnectionLimit() {
    return connectionLimit;
  }

  public int getHttpClientPoolSize() {
    return httpClientPoolSize;
  }

  public int getMaxOutgoingConnPerRoute() {
    return maxOutgoingConnPerRoute;
  }

  public int getMaxOutgoingConn() {
    return maxOutgoingConn;
  }

  public double getPerStorageNodeReadQuotaBuffer() {
    return perStorageNodeReadQuotaBuffer;
  }

  public long getRefreshIntervalForZkReconnectInMs() {
    return refreshIntervalForZkReconnectInMs;
  }

  public int getRefreshAttemptsForZkReconnect() {
    return refreshAttemptsForZkReconnect;
  }

  public boolean isCacheEnabled() {
    return cacheEnabled;
  }

  public long getCacheSizeBytes() {
    return cacheSizeBytes;
  }

  public int getCacheConcurrency() {
    return cacheConcurrency;
  }

  public double getCacheHitRequestThrottleWeight() {
    return cacheHitRequestThrottleWeight;
  }

  public int getRouterNettyGracefulShutdownPeriodSeconds() {
    return routerNettyGracefulShutdownPeriodSeconds;
  }

  public boolean isEnforcingSecureOnly() {
    return enforceSecureOnly;
  }

  public CacheType getCacheType() {
    return cacheType;
  }

  public CacheEviction getCacheEviction() {
    return cacheEviction;
  }

  public int getCacheHashTableSize() {
    return cacheHashTableSize;
  }

  public TreeMap<Integer, Integer> getLongTailRetryForBatchGetThresholdMs() {
    return longTailRetryForBatchGetThresholdMs;
  }

  public boolean isDnsCacheEnabled() {
    return dnsCacheEnabled;
  }

  public String getHostPatternForDnsCache() {
    return hostPatternForDnsCache;
  }

  public long getDnsCacheRefreshIntervalInMs() {
    return dnsCacheRefreshIntervalInMs;
  }

  /**
   * The expected config format is like the following:
   * "1-10:20,11-50:50,51-200:80,201-:1000"
   *
   * @param retryThresholdStr
   * @return
   */
  public static TreeMap<Integer, Integer> parseRetryThresholdForBatchGet(String retryThresholdStr) {
    final String retryThresholdListSeparator = ",\\s*";
    final String retryThresholdSeparator = ":\\s*";
    final String keyRangeSeparator = "-\\s*";
    String[] retryThresholds = retryThresholdStr.split(retryThresholdListSeparator);
    List<String> retryThresholdList = Arrays.asList(retryThresholds);
    // Sort by the lower bound of the key ranges.
    retryThresholdList.sort((range1, range2) -> {
      // Find the lower bound of the key ranges
      String keyRange1[] = range1.split(keyRangeSeparator);
      String keyRange2[] = range2.split(keyRangeSeparator);
      if (keyRange1.length != 2) {
        throw new VeniceException("Invalid single retry threshold config: " + range1 +
            ", which contains two parts separated by '" + keyRangeSeparator + "'");
      }
      if (keyRange2.length != 2) {
        throw new VeniceException("Invalid single retry threshold config: " + range2 +
            ", which should contain two parts separated by '" + keyRangeSeparator + "'");
      }
      return Integer.parseInt(keyRange1[0]) - Integer.parseInt(keyRange2[0]);
    });
    TreeMap<Integer, Integer> retryThresholdMap = new TreeMap<>();
    // Check whether the key ranges are continuous, and store the mapping if everything is good
    int previousUpperBound = 0;
    final int MAX_KEY_COUNT = Integer.MAX_VALUE;
    for (String singleRetryThreshold : retryThresholdList) {
      // parse the range and retry threshold
      String[] singleRetryThresholdParts = singleRetryThreshold.split(retryThresholdSeparator);
      if (singleRetryThresholdParts.length != 2) {
        throw new VeniceException("Invalid single retry threshold config: " + singleRetryThreshold + ", which"
            + " should contain two parts separated by '" + retryThresholdSeparator + "'");
      }
      Integer threshold = Integer.parseInt(singleRetryThresholdParts[1]);
      if (threshold <= 0) {
        throw new VeniceException("Retry threshold should be positive in single retry threshold config: " +
            singleRetryThreshold + ", which should contain a positive retry threshold");
      }
      String[] keyCountRange = singleRetryThresholdParts[0].split(keyRangeSeparator);
      int upperBoundKeyCount = MAX_KEY_COUNT;
      if (keyCountRange.length > 2) {
        throw new VeniceException("Invalid single retry threshold config: " + singleRetryThreshold + ", which"
            + " should contain only lower bound and upper bound of key count range");
      }
      int lowerBoundKeyCount = Integer.parseInt(keyCountRange[0]);
      if (keyCountRange.length == 2) {
        upperBoundKeyCount = keyCountRange[1].isEmpty() ? MAX_KEY_COUNT : Integer.parseInt(keyCountRange[1]);
      }
      if (lowerBoundKeyCount < 0 || upperBoundKeyCount < 0 || lowerBoundKeyCount > upperBoundKeyCount) {
        throw new VeniceException("Invalid single retry threshold config: " + singleRetryThreshold);
      }
      if (lowerBoundKeyCount != previousUpperBound + 1) {
        throw new VeniceException("Current retry threshold config: " + retryThresholdStr +
            " is not continuous according to key count range");
      }
      retryThresholdMap.put(lowerBoundKeyCount, threshold);
      previousUpperBound = upperBoundKeyCount;
    }
    if (!retryThresholdMap.containsKey(1)) {
      throw new VeniceException("Retry threshold for batch-get: " + retryThresholdStr + " should be setup starting from 1");
    }
    if (previousUpperBound != MAX_KEY_COUNT) {
      throw new VeniceException(" Retry threshold for batch-get: " + retryThresholdStr + " doesn't cover unlimited key count");
    }

    return retryThresholdMap;
  }
}
