#!/bin/bash

# ==============================================================================
# Nexus Market — Droplet Setup and Provisioning Script
# Designed for Ubuntu 22.04 LTS / 24.04 LTS
# Run this script as root: sudo bash setup-droplet.sh
# ==============================================================================

set -e

# Colors for logging
GREEN='\033[0;32m'
BLUE='\033[0;34m'
RED='\033[0;31m'
NC='\033[0m' # No Color

echo -e "${BLUE}=== Starting Droplet Provisioning ===${NC}"

# Check root privileges
if [ "$EUID" -ne 0 ]; then
  echo -e "${RED}Error: Please run this script as root or using sudo.${NC}"
  exit 1
fi

# ------------------------------------------------------------------------------
# 1. Configure Swap Space (4GB)
# ------------------------------------------------------------------------------
if swapon --show | grep -q "/swapfile"; then
  echo -e "${GREEN}Swap file already exists.${NC}"
else
  echo -e "${BLUE}Configuring 4GB Swap File (Required for 2GB/4GB RAM Droplets)...${NC}"
  fallocate -l 4G /swapfile
  chmod 600 /swapfile
  mkswap /swapfile
  swapon /swapfile
  echo '/swapfile none swap sw 0 0' >> /etc/fstab
  echo -e "${GREEN}Swap file successfully active.${NC}"
fi

# Show memory report
free -h

# ------------------------------------------------------------------------------
# 2. Update System Packages
# ------------------------------------------------------------------------------
echo -e "${BLUE}Updating system packages...${NC}"
apt-get update -y && apt-get upgrade -y

# ------------------------------------------------------------------------------
# 3. Install Docker Engine and Plugins
# ------------------------------------------------------------------------------
if command -v docker &> /dev/null; then
  echo -e "${GREEN}Docker is already installed.${NC}"
else
  echo -e "${BLUE}Installing Docker...${NC}"
  apt-get install -y ca-certificates curl gnupg lsb-release
  
  mkdir -p /etc/apt/keyrings
  curl -fsSL https://download.docker.com/linux/ubuntu/gpg | gpg --dearmor -o /etc/apt/keyrings/docker.gpg
  
  echo \
    "deb [arch=$(dpkg --print-architecture) signed-by=/etc/apt/keyrings/docker.gpg] https://download.docker.com/linux/ubuntu \
    $(lsb_release -cs) stable" | tee /etc/apt/sources.list.d/docker.list > /dev/null
    
  apt-get update -y
  apt-get install -y docker-ce docker-ce-cli containerd.io docker-buildx-plugin docker-compose-plugin
  
  # Start and enable docker
  systemctl start docker
  systemctl enable docker
  echo -e "${GREEN}Docker engine and plugins installed.${NC}"
fi

# ------------------------------------------------------------------------------
# 4. Install Nginx and Certbot (SSL)
# ------------------------------------------------------------------------------
echo -e "${BLUE}Installing Nginx and Certbot for SSL...${NC}"
apt-get install -y nginx certbot python3-certbot-nginx

# ------------------------------------------------------------------------------
# 5. Configure Host Nginx Site Proxy
# ------------------------------------------------------------------------------
echo -e "${BLUE}Configuring Nginx Reverse Proxy template...${NC}"

# Ask for domain name
read -p "Enter your registered domain (e.g., nexusmarket.me): " DOMAIN_NAME

if [ -z "$DOMAIN_NAME" ]; then
  echo -e "${RED}No domain name provided. Defaulting to 'localhost'. Please update Nginx config manually later.${NC}"
  DOMAIN_NAME="localhost"
fi

NGINX_CONF="/etc/nginx/sites-available/nexus-market"

cat <<EOF > "$NGINX_CONF"
server {
    listen 80;
    server_name $DOMAIN_NAME;

    # Forward all traffic to the Nginx frontend docker container running on port 8080
    location / {
        proxy_pass http://127.0.0.1:8080;
        proxy_set_header Host \$host;
        proxy_set_header X-Real-IP \$remote_addr;
        proxy_set_header X-Forwarded-For \$proxy_add_x_forwarded_for;
        proxy_set_header X-Forwarded-Proto \$scheme;
        
        # Enable WebSocket headers if needed in the future
        proxy_http_version 1.1;
        proxy_set_header Upgrade \$http_upgrade;
        proxy_set_header Connection "upgrade";
    }
}
EOF

# Activate configuration
ln -sf "$NGINX_CONF" /etc/nginx/sites-enabled/default

# Restart Nginx to load changes
systemctl restart nginx
echo -e "${GREEN}Nginx site configuration linked and active.${NC}"

# ------------------------------------------------------------------------------
# Summary Instructions
# ------------------------------------------------------------------------------
echo -e "${GREEN}================================================================${NC}"
echo -e "${GREEN}Provisions completed successfully!${NC}"
echo -e "${GREEN}================================================================${NC}"
echo -e "Next steps to complete deployment:"
echo -e "1. Create your production environmental variables:"
echo -e "   ${BLUE}cp .env.prod.template .env${NC}"
echo -e "   Configure the values inside ${BLUE}.env${NC} using ${BLUE}nano .env${NC}."
echo -e ""
echo -e "2. Compile your Spring Boot microservices locally or on the server:"
echo -e "   ${BLUE}./gradlew build -x test${NC} (runs compilation inside each service directory)"
echo -e ""
echo -e "3. Build and launch your Docker containers in production mode:"
echo -e "   ${BLUE}docker compose -f docker-compose.prod.yml up -d --build${NC}"
echo -e ""
echo -e "4. Provision your free SSL certificate using Certbot:"
echo -e "   ${BLUE}sudo certbot --nginx -d $DOMAIN_NAME${NC}"
echo -e "   (Follow the prompts. Certbot will automatically rewrite Nginx config to use HTTPS!)"
echo -e "================================================================"
