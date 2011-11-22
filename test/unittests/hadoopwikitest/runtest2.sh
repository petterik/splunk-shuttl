#!/usr/bin/env /bin/bash

# start.sh
#
# Copyright (C) 2011 Splunk Inc.
#
# Splunk Inc. licenses this file
# to you under the Apache License, Version 2.0 (the
# "License"); you may not use this file except in compliance
# with the License.  You may obtain a copy of the License at
#
#     http://www.apache.org/licenses/LICENSE-2.0
#
# Unless required by applicable law or agreed to in writing, software
# distributed under the License is distributed on an "AS IS" BASIS,
# WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
# See the License for the specific language governing permissions and
# limitations under the License.
#
$HADOOP_HOME/bin/hadoop jar ../../../build/jar/splunk_hadoop_unittests.jar com.splunk.mapreduce.lib.rest.tests.WikiLinkCount /wordcount/input /wordcount/output$1 $2 $3 $4 $5 $6 
