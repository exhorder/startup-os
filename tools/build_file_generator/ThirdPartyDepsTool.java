/*
 * Copyright 2018 The StartupOS Authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *    https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.google.startupos.tools.buildfilegenerator;

import com.google.startupos.common.CommonModule;
import com.google.startupos.common.FileUtils;
import com.google.startupos.common.flags.Flag;
import com.google.startupos.common.flags.FlagDesc;
import com.google.startupos.common.flags.Flags;
import dagger.Component;

import javax.inject.Singleton;
import java.io.IOException;

import com.google.startupos.tools.buildfilegenerator.Protos.ThirdPartyDeps;

public class ThirdPartyDepsTool {
  @FlagDesc(name = "file_path", description = "Path to file")
  private static Flag<String> filePath =
      Flag.create("/tools/build_file_generator/third_party_deps.prototxt");

  public static void main(String[] args) throws IOException {
    Flags.parseCurrentPackage(args);
    ThirdPartyDepsToolComponent component =
        DaggerThirdPartyDepsTool_ThirdPartyDepsToolComponent.create();

    ThirdPartyDeps thirdPartyDeps = component.getThirdPartyDepsAnalyzer().getThirdPartyDeps();

    FileUtils fileUtils = component.getFileUtils();
    fileUtils.writePrototxtUnchecked(
        thirdPartyDeps, fileUtils.getCurrentWorkingDirectory() + filePath.get());
  }

  @Singleton
  @Component(modules = CommonModule.class)
  interface ThirdPartyDepsToolComponent {
    FileUtils getFileUtils();

    ThirdPartyDepsAnalyzer getThirdPartyDepsAnalyzer();
  }
}

