Matrix <--> E-mail bridge
-------------------------
[![Build Status](https://travis-ci.org/kamax-io/matrix-appservice-email.svg?branch=master)](https://travis-ci.org/kamax-io/matrix-appservice-email)

[Features](#features)  
[Overview](#overview)  
[Requirements](#requirements)  
[Setup](#setup)  
[Integration](#integration)  
[Usage](#usage)

---

This is an E-mail bridge for Matrix using the Application Services (AS) API.

This bridge will pass all Matrix messages from rooms where E-mail virtual users are invited via E-mail, and all E-mail
messages into the corresponding Matrix room.

This software is currently in alpha phase and is not ready for production: Your feedback and ideas are extremely welcome!  
Please help us by opening an issue or joining us on Matrix at
[#mxasd-email:kamax.io](https://matrix.to/#/#mxasd-email:kamax.io)

# Features
- Matrix to E-mail forwarding
- E-mail to Matrix forwarding
- E-mail <-> Matrix <-> E-mail forwarding, if several bridge users are present within a room
- Fully configuration notification templates, per event
- Subscription portal where E-mail users can manage their notifications

# Overview
This bridge will map single virtual users to single e-mail addresses, allowing people without Matrix account to be invited
and participate into Matrix rooms.  
E-mail users can only participate in a room after being invite into it, and cannot subscribe/join on their own.

This bridge does NOT (currently) map entire rooms to a single E-mail address, like a mailing-list.

# Requirements
You will need Java 8 to build and run this bridge.

Due to the current Matrix protocol and various implementations/workflows, this bridge relies on on a custom Identity Server
that proxy to this AS any 3PID requests that have no match.  
This allows a smooth user experience, where someone can always be invited by e-mail using a regular Matrix client use case,
using its Matrix ID if one is found, or inviting a virtual user to directly enable bridging for the given user in the room.

# Setup
Setup can either be done via manually running the bridge or using a Docker image.
You will require Java 1.8 or higher to compile and run this bridge.

## Steps overview
1. [Build the bridge](#build)
2. [Configure the bridge](#configure)
3. [Run the bridge](#run) manually or via Docker
4. [Configure your HS](#homeserver) to read the bridge registration file
5. [See Usage instructions](#usage) to see how to interact with the bridge
6. Start chatting!

## Build
Checkout the repo and initialize the submodules
```
git clone https://github.com/kamax-io/matrix-appservice-email.git
cd matrix-appservice-email
git submodule update --init
```
### Manual
Run the following command in the repo base directory to produce the distribution directory `build/dist/bin` which will
contain all required files and will allow you to test the bridge quickly.  
**It is highly recommended to use a less volatile directory to store those as this directory can be remove with a clean command!**
```
./gradlew buildBin
```

### Docker
Run the following command in the repo base directory to produce a Docker image `kamax.io/mxasd-email`
```
./gradlew buildDocker
```
This command expects you to be able to run `docker` commands without sudo, and without the Gradle daemon running which
will interfere with the build (`permission denied` to connect to the Docker socket).

If you cannot fulfil these requirements and need to manually build the Docker image, prepare the staging directory:
```
./gradlew buildBin
cp src/main/docker/Dockerfile build/dist/bin
```
You can then run your usual build command pointing to `build/dist/bin` like so
```
sudo docker build build/dist/bin -t kamax.io/mxasd-email
```

## Configure
Copy the default config file located in `src/main/resources/application.yaml` into a permanent directory depending on your build type:
- In `build/dist/bin` by default if you went for the manual build method, but **you should use a less volatile location**
- A dedicated directory which will be presented as a volume, if you went for the Docker build method, e.g. `/data/mxasd-email`

The configuration file contains a detailed description for each possible configuration item.

## Run
### Manual
Change into the directory containing the main jar (`build/dist/bin` by default) and run it:
```
cd build/dist/bin
./matrix-appservice-email.jar
```

### Docker
Run a new container for the newly created image, and make sure `/data/mxasd-email` is adapted to your actual location:
```
docker run -p 8091:8091 -v /data/mxasd-email:/data kamax.io/mxasd-email
```
# Integration
## Homeserver
Like any bridge, a registration file must be generated which will then be added to the HS config.  
Currently, there is no mechanism to automatically generate this config file.

You will find a working example at `registration-sample.yaml`, which you should copy at the same location as the Bridge configuration file.  
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

# Usage
## Via Identity Server
1. [Install and Configure mxisd](#mxisd) to use this bridge for unknown 3PID lookup
2. Configure your Matrix client to use mxisd as the Identity Server
3. Invite someone to a room with an e-mail which has no 3PID mapping. In Riot, use "Invite to this room" feature.

## Manual
1. Invite the AS user to a new chat or in an existing room (replace the Matrix ID to the appropriate value):
```
/invite @appservice-email:localhost`
```

2. Send the command into the room/chat to get the Matrix ID for a given e-mail (replace with appropriate value):
```
!email mxid john.doe@example.org
```
You should get an answer from the AS user, which will look like this:
```
MXID for john.doe@example.org:

   @email_john.doe=40example.org:localhost
```

3. Use the given MAtrix ID  to invite the Bridge user into a room or chat.
4. The E-mail user will receive a notification of the next conversation.
5. Start chatting!