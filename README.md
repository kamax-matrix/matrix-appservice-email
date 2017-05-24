Matrix <--> E-mail bridge
-------------------------
[![Build Status](https://travis-ci.org/kamax-io/matrix-appservice-email.svg?branch=master)](https://travis-ci.org/kamax-io/matrix-appservice-email)

This is an E-mail bridge for Matrix using the Application Services (AS) API.

This bridge will pass all Matrix messages from rooms where E-mail virtual users are invited via E-mail, and all E-mail
messages into the corresponding Matrix room.

This software is currently in alpha phase and is not ready for production.
Your feedback and ideas are extremely welcome - please help us by opening an issue or joining us on Matrix at
[#mxasd-email:kamax.io](https://matrix.to/#/#mxasd-email:kamax.io)

# Features
- Matrix to E-mail forwarding
- E-mail to Matrix forwarding
- E-mail <-> Matrix <-> E-mail forwarding, if several bridge users are present within a room
- Fully configuration notification templates, per event
- Subscription portal where E-mail users can manage their notifications

# Requirements
You will need Java 8 to build and run this bridge.

Due to the current Matrix protocol and various implementations/workflows, this bridge relies on on a custom Identity Server
that proxy to this AS any 3PID requests that have no match.  
This allows a smooth user experience, where someone can always be invited by e-mail using a regular Matrix client use case,
using its Matrix ID if one is found, or inviting a virtual user to directly enable bridging for the given user in the room.

# Setup
Setup can either be done via manually running the bridge or using a Docker image.
You will require Java 1.8 or higher to compile and run this bridge.

## Overview
1. [Build the bridge](#build)
2. [Configure the bridge](#configure)
3. [Run the bridge](#run) manually or via Docker
4. [Configure your HS](#homeserver) to read the bridge registration file
5. [Install and Configure mxisd](#mxisd) to use this bridge for unknown 3PID lookup
7. Configure your Matrix client to use mxisd as the Identity Server
8. Invite someone to a room with an e-mail which has no 3PID mapping
9. Start chatting!

## Build
Checkout the repo and initialize the submodules
```
git clone https://github.com/kamax-io/matrix-appservice-email.git
cd matrix-appservice-email
git submodule update --init
```
### Manual
Run the following command in the repo base directory to produce a distribution directory which will contain all required files and cd into it:
```
./gradlew distBin
cd build/dist/bin
```

### Docker
Run the following command in the repo base directory to produce a Docker image `kamax.io/matrix-appservice-email`
```
./gradlew buildDocker
```
This command expects you to be able to run `docker` commands without sudo, and without the Gradle daemon running which
will interfere with the build (`permission denied` to connect to the Docker socket).

If you cannot fulfil these requirements and need to manually build the Docker image, prepare the staging directory:
```
./gradlew build distBin
cp sec/main/docker/Dockerfile build/dist/bin
```
You can then run your usual build command pointing to `build/dist/bin` like so
```
sudo docker build build/dist/bin -t kamax.io/mxasd-email
```

## Configure
With your favorite editor, open `application.yaml` which should be either:
- In `build/dist/bin` if you went for the manual build method
- Copied to a dedicated directory which will be presented as a volume, if you went for the Docker build method

While the configuration file is quite extensive, only the `matrix` and `email` sections need to be configured to run the bridge.  
To run behind a reverse proxy, make sure the `server.host` key is configured (commented out by default)

If you would like to use a dedicated configuration file for your changes, you can use the Spring configuration profiles feature.  
This will allow you to mix both configuration file, keeping default config values and only overwriting those of interest.  
Replacing `<profile_name>` by an arbitrary profile name (except `default`), e.g. `main`, create a new config file next to
the default file and populate it with only the relevant sections.  
This mechanism is explained in more details in the [Spring Boot documentation](https://docs.spring.io/spring-boot/docs/current/reference/html/boot-features-external-config.html#boot-features-external-config-profile-specific-properties)

For example, you could have `application-main.yaml` which only contains the following:
```
email:
  template: "matrix-appservice-email+%KEY%@example.org"

  receiver:
    type: "imaps"
    host: "imap.example.org"
    port: 993
    login: "matrix-appservice-email@example.org"
    password: "mypassword"

  sender:
    host: "smtp.example.org"
    port: 587
    tls: 1
    login: "matrix-appservice-email"
    password: "mypassword"
    email: "matrix-appservice-email@example.org"
    name: "Matrix E-mail Bridge"
```
It is also possible to add profiles to the main config file. For more information, see the [Spring Boot documentation](https://docs.spring.io/spring-boot/docs/current/reference/html/boot-features-profiles.html#boot-features-adding-active-profiles)

## Run
After editing the configuration file, you can now run the bridge.  
Follow the steps depending on your build path.

### Manual
If you edited the default config file:
```
./matrix-appservice-email.jar
```

If you use a profile configuration:
```
./matrix-appservice-email.jar --spring.profiles.active=<profile_name>
```

### Docker
If you edited the default config file:
```
docker run -p 8091:8091 -v /path/to/data:/data kamax.io/mxasd-email
```

If you use a profile configuration:
```
docker run -p 8091:8091 -v /path/to/data:/data -e "SPRING_PROFILES_ACTIVE=<profile_name>" kamax.io/mxasd-email
```
# Integration
## Homeserver
Like any bridge, a registration file must be generated which will then be added to the HS config.  
Currently, there is no mechanism to automatically generate this config file.

You can find a working example [here](https://raw.githubusercontent.com/kamax-io/matrix-appservice-email/master/registration-sample.yaml), which you should copy at the same location as the Bridge configuration file.  
Configuration must match the `matrix` section in the bridge config file.

The Homeserver can then be configured with:
```
app_service_config_files:
    - "/path/to/registration.yaml"
```

## mxisd
Follow instructions on the [git repo README](https://github.com/kamax-io/mxisd/blob/master/README.md) until you have a working setup.

Add/edit the following sections in the mxisd configuration file, adapt the AS URL, and restart if needed:
```
lookup:
  recursive:
    bridge:
      enabled: true
      mappings:
        email: 'http://localhost:8091'
```
