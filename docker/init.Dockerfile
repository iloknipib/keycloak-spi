FROM registry.access.redhat.com/ubi8-minimal
LABEL maintainer="Subhash Samota <subhash.samota@agrevolution.in>"
RUN microdnf update && microdnf clean all && rm -rf /var/cache/yum/*
CMD ["/bin/echo", "All set!!"]