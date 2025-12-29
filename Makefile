.PHONY: help build run test clean docker-up docker-down docker-logs

# Default target
.DEFAULT_GOAL := help

# Variables
MVN := mvn
DOCKER_COMPOSE := docker-compose

help: ## Show this help message
	@echo 'Usage: make [target]'
	@echo ''
	@echo 'Available targets:'
	@awk 'BEGIN {FS = ":.*?## "} /^[a-zA-Z_-]+:.*?## / {printf "  \033[36m%-20s\033[0m %s\n", $$1, $$2}' $(MAKEFILE_LIST)

build: ## Build the project
	@echo "Building project..."
	$(MVN) clean package -DskipTests

run: ## Run the application
	@echo "Running application..."
	$(MVN) spring-boot:run -pl archbase-boilerplate-rest

test: ## Run tests
	@echo "Running tests..."
	$(MVN) test

clean: ## Clean build artifacts
	@echo "Cleaning..."
	$(MVN) clean

docker-up: ## Start Docker containers
	@echo "Starting Docker containers..."
	$(DOCKER_COMPOSE) up -d postgres redis adminer redisinsight

docker-down: ## Stop Docker containers
	@echo "Stopping Docker containers..."
	$(DOCKER_COMPOSE) down

docker-logs: ## Show Docker logs
	$(DOCKER_COMPOSE) logs -f

docker-ps: ## Show running Docker containers
	$(DOCKER_COMPOSE) ps

rebuild: docker-down clean docker-up build ## Rebuild everything (docker + project)

install: ## Install dependencies
	@echo "Installing dependencies..."
	$(MVN) clean install -DskipTests

package: ## Create deployment package
	@echo "Creating package..."
	$(MVN) clean package

dev: docker-up ## Start development environment (docker + app)
	@echo "Development environment started!"
	@echo "PostgreSQL: localhost:5432"
	@echo "Redis: localhost:6379"
	@echo "Adminer: http://localhost:8081"
	@echo "RedisInsight: http://localhost:8001"
	@echo ""
	@echo "Run 'make run' to start the application"
