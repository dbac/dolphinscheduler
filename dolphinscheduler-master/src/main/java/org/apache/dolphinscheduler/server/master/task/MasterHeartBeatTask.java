/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.apache.dolphinscheduler.server.master.task;

import org.apache.dolphinscheduler.common.enums.ServerStatus;
import org.apache.dolphinscheduler.common.lifecycle.ServerLifeCycleManager;
import org.apache.dolphinscheduler.common.model.BaseHeartBeatTask;
import org.apache.dolphinscheduler.common.model.MasterHeartBeat;
import org.apache.dolphinscheduler.common.utils.JSONUtils;
import org.apache.dolphinscheduler.common.utils.NetUtils;
import org.apache.dolphinscheduler.common.utils.OSUtils;
import org.apache.dolphinscheduler.registry.api.RegistryClient;
import org.apache.dolphinscheduler.server.master.config.MasterConfig;

import lombok.NonNull;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class MasterHeartBeatTask extends BaseHeartBeatTask<MasterHeartBeat> {

    private final MasterConfig masterConfig;

    private final RegistryClient registryClient;

    private final String heartBeatPath;

    private final int processId;

    public MasterHeartBeatTask(@NonNull MasterConfig masterConfig,
                               @NonNull RegistryClient registryClient) {
        super("MasterHeartBeatTask", masterConfig.getHeartbeatInterval().toMillis());
        this.masterConfig = masterConfig;
        this.registryClient = registryClient;
        this.heartBeatPath = masterConfig.getMasterRegistryPath();
        this.processId = OSUtils.getProcessID();
    }

    @Override
    public MasterHeartBeat getHeartBeat() {
        return MasterHeartBeat.builder()
                .startupTime(ServerLifeCycleManager.getServerStartupTime())
                .reportTime(System.currentTimeMillis())
                .cpuUsage(OSUtils.cpuUsagePercentage())
                .availablePhysicalMemorySize(OSUtils.availablePhysicalMemorySize())
                .reservedMemory(masterConfig.getReservedMemory())
                .memoryUsage(OSUtils.memoryUsagePercentage())
                .diskAvailable(OSUtils.diskAvailable())
                .processId(processId)
                .serverStatus(getServerStatus())
                .host(NetUtils.getHost())
                .port(masterConfig.getListenPort())
                .build();
    }

    @Override
    public void writeHeartBeat(MasterHeartBeat masterHeartBeat) {
        String masterHeartBeatJson = JSONUtils.toJsonString(masterHeartBeat);
        registryClient.persistEphemeral(heartBeatPath, masterHeartBeatJson);
        log.debug("Success write master heartBeatInfo into registry, masterRegistryPath: {}, heartBeatInfo: {}",
                heartBeatPath, masterHeartBeatJson);
    }

    private ServerStatus getServerStatus() {
        return OSUtils.isOverload(masterConfig.getMaxCpuLoadAvg(), masterConfig.getReservedMemory())
                ? ServerStatus.ABNORMAL
                : ServerStatus.NORMAL;
    }
}
