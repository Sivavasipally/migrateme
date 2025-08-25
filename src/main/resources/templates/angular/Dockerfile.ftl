# Multi-stage Dockerfile for ${name} (Angular)
# Stage 1: Build the Angular application
FROM node:18-alpine AS builder

# Set working directory
WORKDIR /app

# Copy package files
COPY package*.json ./

# Install dependencies
RUN npm ci

# Copy source code
COPY . .

# Build the application
RUN npm run build --prod

# Stage 2: Serve with nginx
FROM nginx:alpine AS runtime

# Copy nginx configuration
COPY nginx.conf /etc/nginx/nginx.conf

# Copy built Angular app from builder stage
COPY --from=builder /app/dist/${artifactId} /usr/share/nginx/html

# Create non-root user for security
RUN addgroup -g 1001 -S nodejs && adduser -S angularuser -u 1001 -G nodejs

# Change ownership
RUN chown -R angularuser:nodejs /usr/share/nginx/html
RUN chown -R angularuser:nodejs /var/cache/nginx
RUN chown -R angularuser:nodejs /var/log/nginx
RUN chown -R angularuser:nodejs /etc/nginx/conf.d
RUN chown -R angularuser:nodejs /run

# Switch to non-root user
USER angularuser

# Expose port
EXPOSE 80

# Health check
HEALTHCHECK --interval=30s --timeout=3s --start-period=60s --retries=3 \
  CMD wget --no-verbose --tries=1 --spider http://localhost/ || exit 1

# Start nginx
CMD ["nginx", "-g", "daemon off;"]