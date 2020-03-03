FROM jenkins/slave:alpine
MAINTAINER Carlos Rodriguez Lopez <it.carlosrodlop@gmail.com>

# See https://github.com/docker-library/openjdk/blob/master/8/jre/alpine/Dockerfile
ARG JRE_VERSION=8.212.04-r1
ARG AWS_CLI_VERSION=1.16.139
ARG S3_CMD_VERSION=2.0.2

# Install AWS cli (see https://github.com/alecharp/build-tools/blob/docker-aws/Dockerfile)
RUN apk add --update python py-pip && \
    pip install --no-cache-dir --upgrade pip awscli==$AWS_CLI_VERSION s3cmd==$S3_CMD_VERSION && \
    apk -v --purge del py-pip && \
    rm /var/cache/apk/*
