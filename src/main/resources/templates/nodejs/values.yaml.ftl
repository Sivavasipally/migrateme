# Default values for ${artifactId}
# This is a YAML-formatted file.

replicaCount: 2

image:
  repository: ${artifactId}
  pullPolicy: IfNotPresent
  tag: "${version}"

imagePullSecrets: []
nameOverride: ""
fullnameOverride: ""

serviceAccount:
  create: true
  annotations: {}
  name: ""

podAnnotations: {}

podSecurityContext:
  fsGroup: 1001
  runAsNonRoot: true
  runAsUser: 1001

securityContext:
  allowPrivilegeEscalation: false
  capabilities:
    drop:
    - ALL
  readOnlyRootFilesystem: false
  runAsNonRoot: true
  runAsUser: 1001

service:
  type: ClusterIP
  port: 80
  targetPort: ${serverPort}

ingress:
  enabled: true
  className: ""
  annotations:
    kubernetes.io/ingress.class: nginx
    nginx.ingress.kubernetes.io/ssl-redirect: "false"
    cert-manager.io/cluster-issuer: letsencrypt-prod
  hosts:
    - host: ${artifactId}.example.com
      paths:
        - path: /
          pathType: Prefix
  tls:
    - secretName: ${artifactId}-tls
      hosts:
        - ${artifactId}.example.com

resources:
  limits:
    cpu: 1000m
    memory: 1Gi
  requests:
    cpu: 250m
    memory: 512Mi

autoscaling:
  enabled: true
  minReplicas: 2
  maxReplicas: 10
  targetCPUUtilizationPercentage: 70
  targetMemoryUtilizationPercentage: 80

nodeSelector: {}

tolerations: []

affinity:
  podAntiAffinity:
    preferredDuringSchedulingIgnoredDuringExecution:
      - weight: 100
        podAffinityTerm:
          labelSelector:
            matchExpressions:
              - key: app.kubernetes.io/name
                operator: In
                values:
                  - ${artifactId}
          topologyKey: kubernetes.io/hostname

# Spring Boot specific configuration
springboot:
  profile: production
  
# Environment variables
env:
  - name: SPRING_PROFILES_ACTIVE
    value: "production"
  - name: SERVER_PORT
    value: "${serverPort}"

# ConfigMap and Secret references
configMapRefs: []
secretRefs: []

# Probes configuration
livenessProbe:
  httpGet:
    path: /actuator/health/liveness
    port: ${serverPort}
  initialDelaySeconds: 60
  periodSeconds: 30
  timeoutSeconds: 5
  failureThreshold: 3

readinessProbe:
  httpGet:
    path: /actuator/health/readiness
    port: ${serverPort}
  initialDelaySeconds: 30
  periodSeconds: 10
  timeoutSeconds: 5
  failureThreshold: 3

startupProbe:
  httpGet:
    path: /actuator/health/liveness
    port: ${serverPort}
  initialDelaySeconds: 30
  periodSeconds: 10
  timeoutSeconds: 5
  failureThreshold: 30