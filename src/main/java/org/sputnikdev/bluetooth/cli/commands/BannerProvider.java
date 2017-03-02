package org.sputnikdev.bluetooth.cli.commands;

/*-
 * #%L
 * org.sputnikdev:bluetooth-cli
 * %%
 * Copyright (C) 2017 Sputnik Dev
 * %%
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * 
 *      http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 * #L%
 */

import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.shell.plugin.support.DefaultBannerProvider;
import org.springframework.shell.support.util.OsUtils;
import org.springframework.stereotype.Component;

/**
 *
 * @author Vlad Kolotov
 */
@Component
@Order(Ordered.HIGHEST_PRECEDENCE)
public class BannerProvider extends DefaultBannerProvider  {

    public String getBanner() {
        return
                "=======================================" + OsUtils.LINE_SEPARATOR +
                "*                                     *" + OsUtils.LINE_SEPARATOR +
                "*            Bluetooth Manager        *" + OsUtils.LINE_SEPARATOR +
                "*                                     *" + OsUtils.LINE_SEPARATOR +
                "=======================================" + OsUtils.LINE_SEPARATOR +
                "Version:" + this.getVersion();
    }

    public String getVersion() {
        return "0.1";
    }

    public String getWelcomeMessage() {
        return "Welcome to Bluetooth Manager";
    }

    @Override
    public String getProviderName() {
        return "Bluetooth Manager Banner";
    }
}
