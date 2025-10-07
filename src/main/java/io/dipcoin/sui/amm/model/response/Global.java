/*
 * Copyright 2025 Dipcoin LLC
 *
 * Licensed under the Apache License, Version 2.0 (the "License");you may not use this file except in compliance with
 * the License.You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,software distributed under the License is distributed on
 * an "AS IS" BASIS,WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.See the License for the
 * specific language governing permissions and limitations under the License.
 */

package io.dipcoin.sui.amm.model.response;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * @author : Same
 * @datetime : 2025/10/5 12:15
 * @Description : Global protocol configuration
 */
@AllArgsConstructor
@NoArgsConstructor
@Data
public class Global {

    /** Global config ID */
    private String id;

    /** Whether protocol is paused */
    @JsonProperty("has_paused")
    private boolean hasPaused;

    /** Whether protocol fee is enabled */
    @JsonProperty("is_open_protocol_fee")
    private boolean isOpenProtocolFee;

}
