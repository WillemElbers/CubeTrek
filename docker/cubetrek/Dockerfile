FROM alpine:3.21.2 AS base

# Install base packages
ENV OPENSSL_VERSION=3.3.2-r5
ENV MUSL_LOCPATH=/usr/share/i18n/locales/musl

ARG BASH_VERSION=5.2.37-r0
ARG CURL_VERSION=8.12.0-r0
ARG ZIP_VERSION=3.0-r13
ARG UNZIP_VERSION=6.0-r15

RUN apk add --no-cache \
        "curl=$CURL_VERSION" \
        "libcrypto3=$OPENSSL_VERSION" \
        "libssl3=$OPENSSL_VERSION" \
        "bash=$BASH_VERSION" \
        "zip=$ZIP_VERSION" \
        "unzip=$UNZIP_VERSION"

# Install openjdk (jre only)

ENV JAVA_HOME /usr/lib/jvm/default-jvm
ENV PATH $PATH:/usr/lib/jvm/default-jvm/jre/bin:/usr/lib/jvm/default-jvm/bin

ENV GRADLE_VERSION=8.11.1-r0
ENV JAVA_VERSION 21.0.5
ENV JAVA_ALPINE_VERSION=21.0.6_p7-r0
ARG CA_CERTIFICATES_VERSION=20241121-r1

RUN set -x \
  && env \
  && apk add --no-cache \
      "openjdk21-jre=$JAVA_ALPINE_VERSION" \
      "ca-certificates=$CA_CERTIFICATES_VERSION" \
      "openssl=$OPENSSL_VERSION"


######################################################
# Library build stage
######################################################

FROM base AS build

#Build and install musl locales
#https://grrr.tech/posts/2020/add-locales-to-alpine-linux-docker-image/

ARG CMAKE_VERSION=3.31.1-r0
ARG GCC_VERSION=14.2.0-r4
ARG GETTEXT_DEV_VERSION=0.22.5-r0
ARG LIBINTL_VERSION=0.22.5-r0
ARG MAKE_VERSION=4.4.1-r2
ARG MUSL_DEV_VERSION=1.2.5-r8
ARG MUSL_LOCALES_REV=1101fb2bcdd189cd9415b8bd1c775eb43527d25c
ARG GIT_VERSION=2.47.2-r0

SHELL ["/bin/bash", "-o", "pipefail", "-c"]

# hadolint ignore=DL3003
RUN apk add --no-cache \
        "cmake=$CMAKE_VERSION" \
        "make=$MAKE_VERSION" \
        "musl-dev=$MUSL_DEV_VERSION" \
        "gcc=$GCC_VERSION" \
        "gettext-dev=$GETTEXT_DEV_VERSION" \
        "libintl=$LIBINTL_VERSION" \
    && curl -sSL -o 'musl-locales.zip' \
    	"https://gitlab.com/rilian-la-te/musl-locales/-/archive/${MUSL_LOCALES_REV}/musl-locales-${MUSL_LOCALES_REV}.zip" \
    && unzip 'musl-locales.zip' \
    && cd "$(find . -type d -name 'musl-locales-*')" \
    && cmake -DLOCALE_PROFILE=OFF -D CMAKE_INSTALL_PREFIX:PATH=/usr . \
    && make \
    && make install

#Get and extract renderizer
#Reference: https://github.com/gomatic/renderizer

ARG RENDERIZER_VERSION=2.0.13
ARG RENDERIZER_CHECKSUM="d6a8f045cb6fe6c3d43e44a2f307cdce86c2c6911b3179e6a46a47dc6cef738a"

RUN curl -sSL -o '/tmp/renderizer_linux_x86_64.tar.gz' "https://github.com/gomatic/renderizer/releases/download/v${RENDERIZER_VERSION}/renderizer_linux_x86_64.tar.gz" \
 && echo "${RENDERIZER_CHECKSUM}"'  /tmp/renderizer_linux_x86_64.tar.gz' | sha256sum -c - \
 && tar -x -C '/usr/bin' -f '/tmp/renderizer_linux_x86_64.tar.gz' \
 && test -x '/usr/bin/renderizer'


#Build CubeTrek and TopoLibrary

#Install dependencies
RUN apk add --no-cache "openjdk21=$JAVA_ALPINE_VERSION" \
    "gradle=$GRADLE_VERSION" \
    "git=$GIT_VERSION"

#Download and extract FIT sdk
WORKDIR /tmp
RUN curl -sSL -o "/tmp/FitSDKRelease_21.158.00.zip" "https://developer.garmin.com/downloads/fit/sdk/FitSDKRelease_21.158.00.zip" \
 && mkdir fitsdk \
 && cd fitsdk \
 && unzip -q /tmp/FitSDKRelease_21.158.00.zip

#Build TopoLibrary
WORKDIR /tmp
RUN git clone https://github.com/r-follador/TopoLibrary.git \
 && cd TopoLibrary \
 && git checkout 585f139 \
 && mkdir -p /tmp/TopoLibrary/libs \
 && cp /tmp/fitsdk/java/fit.jar /tmp/TopoLibrary/libs/fit.jar

WORKDIR /tmp/TopoLibrary
RUN sed -i "s/'2.3-SNAPSHOT'/'2.3-SNAPSHOT2'/g" build.gradle \
 && gradle --no-daemon dependencies \
 && gradle --no-daemon build -x test

#Build CubeTrek
WORKDIR /tmp
RUN curl -sSL -o "/tmp/cubetrek.tar.gz" "https://github.com/WillemElbers/CubeTrek/archive/refs/tags/v5.4.tar.gz" \
 && tar -xf "/tmp/cubetrek.tar.gz" \
 && mkdir /tmp/CubeTrek-5.4/libs \
 && cp /tmp/fitsdk/java/fit.jar /tmp/CubeTrek-5.4/libs/fit.jar \
 && cp /tmp/TopoLibrary/build/libs/TopoLibrary-2.3-SNAPSHOT2.jar /tmp/CubeTrek-5.4/libs/

WORKDIR /tmp/CubeTrek-5.4
RUN sed -i 's/\/home\/rainer\/Software_Dev\/IdeaProjects\/TopoLibrary\/build\/libs\/TopoLibrary-2.3-SNAPSHOT.jar/.\/libs\/TopoLibrary-2.3-SNAPSHOT2.jar/g' build.gradle \
 && sed -i 's/\/home\/rainer\/Software_Dev\/IdeaProjects\/TopoLibrary\/libs\/fit.jar/.\/libs\/fit.jar/g' build.gradle \
 && gradle --no-daemon dependencies \
 && gradle --no-daemon build -x test

#######################################################
## Base image
#######################################################

FROM base

COPY --from=build /usr/share/i18n/locales/musl /usr/share/i18n/locales/musl
COPY --from=build /usr/share/locale /usr/share/locale
COPY --from=build /usr/bin/locale /usr/bin/locale
COPY --from=build /usr/bin/renderizer /usr/bin/renderizer
COPY --from=build /tmp/CubeTrek-5.4/build/libs/cubetrek-1.1-SNAPSHOT.jar /app/cubetrek-1.1-SNAPSHOT.jar

ENV LANG=en_US.UTF-8
ENV CHARSET=UTF-8
ENV LC_ALL=en.US.UTF-8

# Expose the port for the application
EXPOSE 8080

# Set the entry point to run the application
WORKDIR /app
#Mount custom
VOLUME /app/config
#Persist logs
VOLUME /logs
# Mount hgt files
VOLUME /maps
#java -jar cubetrek-1.1-SNAPSHOT.jar --spring.config.location=/app/config/
ENTRYPOINT ["java", "-jar", "cubetrek-1.1-SNAPSHOT.jar", "--spring.config.location=/app/config/"]
