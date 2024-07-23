#
# Copyright (c) 2021 IBA Group, a.s. All rights reserved.
#
# Licensed to the Apache Software Foundation (ASF) under one or more
# contributor license agreements.  See the NOTICE file distributed with
# this work for additional information regarding copyright ownership.
# The ASF licenses this file to You under the Apache License, Version 2.0
# (the "License"); you may not use this file except in compliance with
# the License.  You may obtain a copy of the License at
#
#   http://www.apache.org/licenses/LICENSE-2.0
#
# Unless required by applicable law or agreed to in writing, software
# distributed under the License is distributed on an "AS IS" BASIS,
# WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
# See the License for the specific language governing permissions and
# limitations under the License.
#
FROM public.ecr.aws/lambda/java:17

RUN yum update -y && yum install -y openssl && yum clean all && rm -rf /var/cache/yum

COPY ./target/VF-databricks.jar generate_keystore_p12.sh /app/
COPY spark-transformations-0.1-jar-with-dependencies.jar /app/
COPY spark-transformations-0.1-jar-with-dependencies.jar.md5 /app/

WORKDIR /app/

ENTRYPOINT ["/bin/sh", "-c", "sh ./generate_keystore_p12.sh; java -Xms1g -Xmx8g -jar VF-databricks.jar --spring.config.location=file:/config/application.yaml"]
