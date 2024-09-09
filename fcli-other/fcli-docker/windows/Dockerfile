# This Dockerfile allows for building a Windows-based fcli image. Although functional, for now 
# this is considered just a prototype and not automatically published; we can fine-tune and 
# publish if there's any demand for a Windows-based image. Note that may suffer from some of 
# the same issues regarding base OS CVEs and ability run run ScanCentral Client as detailed in 
# the Linux Dockerfile.
#
# Some questions to be answered (also see more detailed comments on the actual Dockerfile instructions):
# - What base image to use for the final image; nanoserver, servercore, ...?
# - Any improvements necessary for VC++ redistributables installation?
# - Should container run as ContainerUser, or should we create fcli user like for the Linux images?
# - Should we have fcli.exe or CMD.exe as entry point (or both)?
# 
# Before running the 'docker build' command, the fcli Windows executable to be included in the 
# image must be copied into the same directory as this Dockerfile. If we decide to automatically
# release Windows-based images, the fcli build process will automatically take care of this when 
# releasing a new fcli version. For manual testing of this Dockerfile, you can safely copy fcli.exe 
# 2.5.4 or above into your workspace; the .gitignore file will have git operations ignore the fcli 
# executable.

# Install Visual C++ redistributables; this requires at least servercore and will fail on nanoserver,
# hence we do this in a separate build phase.
FROM mcr.microsoft.com/windows/servercore:ltsc2022 as installer
ADD https://aka.ms/vs/17/release/vc_redist.x64.exe /vc_redist.x64.exe
RUN /vc_redist.x64.exe /install /quiet /norestart

# Build fcli nanoserver-based image. We manually copy any VC++ DLLs required by fcli into this image
# from the installer phase above. This process is somewhat fragile and may break the image if a future 
# fcli version requires additional VC++ DLLs but we forget to update the Dockerfile to copy them. We 
# may want to add an instruction like 'RUN fcli -h' instruction after installing fcli, to ensure fcli 
# can run successfully with the manually copied set of DLLs.
FROM mcr.microsoft.com/windows/nanoserver:ltsc2022 AS fcli-ltsc2022
COPY --from=installer C:/Windows/System32/vcruntime140.dll C:/Windows/System32/vcruntime140.dll
COPY --from=installer C:/Windows/System32/vcruntime140_1.dll C:/Windows/System32/vcruntime140_1.dll
ADD fcli.exe C:/bin/fcli.exe
ADD data.tgz C:/
WORKDIR C:/data
ENV FCLI_USER_HOME=C:/data
USER ContainerUser
ENV PATH=$PATH:C:\bin
CMD ["cmd.exe"]
