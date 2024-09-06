# This Dockerfile defines the following targets for building an fcli fcli Docker image:
# - fcli-alpine
# - fcli-ubi9
# - fcli-scratch
# You can select between these variants by passing the --target option to the 'docker build'
# command, for example 'docker build . --target fcli-alpine'. If no target is specified, all
# targets may be built but the final image will correspond to the fcli-scratch target.
#
# In general, the fcli-scratch image is preferred to avoid having to deal with any CVEs reported
# on the base images, and potentially having to provide refreshed images for older fcli versions
# to fix such CVEs. Potentially, we could add commands to upgrade all base OS packages to reduce
# the number of CVEs on those packages, in case the base image doesn't include those upgraded
# packages yet.
#
# For now, we're only automatically publishing the fcli-scratch image for the reasons above; if
# there's any demand, we can also publish ubi9, alpine and/or other (like busybox) images that 
# include a shell. Primary advantage is that this would allow for running a sequence of fcli 
# commands in a single container invocation (potentially removing the need to mount the data 
# directory to a persistent volume/host directory, as fcli state will be stored inside the container 
# for as long the container is alive). Primary disadvantage is that users might expect to be able to 
# run something like the following:
#   fcli tool sc-client install
#   scancentral package ...
#   submit scan request, ...
# However, these images don't include Java (required to run ScanCentral Client) nor any other
# build tools (which may be required by ScanCentral Client to properly package application source
# code). Users would need to build a custom image that includes fcli and any other tools required
# for building/packaging an application, or alternatively any of the fcli Docker images can be
# used to install ScanCentral Client on the host system (using proper volume mappings), from
# where it can be run with (presumably) all relevant build tools and Java being available.
# 
# Before running the 'docker build' command, the fcli executable to be included in the image
# must be copied into the same directory as this Dockerfile. The fcli build process will 
# automatically take care of this when releasing a new fcli version. For manual testing of
# this Dockerfile, you can safely copy fcli 2.5.4 or above into your workspace; the .gitignore
# file will have git operations ignore the fcli executable.
#
# All of the targets defined below have more or less the same structure:
# - Add fcli user to /etc/passwd
# - Add fcli executable to the image
# - Add /data (and /tmp for fcli-scratch) directories with 777 permissions to the image
# - Set WORKDIR to /data
# - Set FCLI_USER_HOME to /data
# - Set container user to fcli user
# - Define an appropriate entrypoint (fcli for fcli-scratch, shell for others)
#
# Regarding FCLI_USER_HOME, fcli uses user.home by default, but if the Docker container is being
# run as a different user through 'docker run -u <UID>:<GID>', user.home returns a directory 
# containing a '?' to represent unknown user name for the given UID, which causes issues. Fcli
# versions 2.5.4 and up allow for overriding user.home through FCLI_USER_HOME. 

FROM alpine:3 AS fcli-alpine
RUN echo "fcli:x:10001:10001:fcli:/data:/sbin/nologin" >> /etc/passwd
ADD fcli /bin/fcli
ADD data.tgz /
WORKDIR /data
ENV FCLI_USER_HOME=/data
USER fcli
ENTRYPOINT ["/bin/sh"]

FROM redhat/ubi9-minimal:9.4 AS fcli-ubi9
RUN echo "fcli:x:10001:10001:fcli:/data:/sbin/nologin" >> /etc/passwd
ADD fcli /bin/fcli
ADD data.tgz /
WORKDIR /data
ENV FCLI_USER_HOME=/data
USER fcli
ENTRYPOINT ["/bin/bash"]

FROM scratch AS fcli-scratch
ADD minimal-passwd /etc/passwd
ADD fcli /bin/fcli
ADD tmp.tgz /
ADD data.tgz /
WORKDIR /data
ENV FCLI_USER_HOME=/data
USER fcli
ENTRYPOINT ["/bin/fcli"]
