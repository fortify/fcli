This module contains patched picocli classes, copied from https://github.com/fortify-ps/picocli, to fix the following issues:
- https://github.com/remkop/picocli/issues/1696
- https://github.com/remkop/picocli/issues/1706

Once the fixes for these issues have been incorporated into a new picocli release, this module can be removed, and the following
files in the fcli main module should be updated:
- Remove fcli-picocli-patch module from settings.gradle
- Remove all references to fcli-picocli-patch from build.gradle
- Re-enable the normal picocli dependency
