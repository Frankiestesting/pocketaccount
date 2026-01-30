#!/bin/bash
# Start PocketAccount Backend and Frontend

set -e

SCRIPT_DIR="$( cd "$( dirname "${BASH_SOURCE[0]}" )" && pwd )"
cd "$SCRIPT_DIR"

echo "📦 Starting PocketAccount..."
echo ""

# Set environment variable for OpenAI (required)
export OPENAI_API_KEY="${OPENAI_API_KEY:-sk-test-placeholder}"

# Start Backend
echo "🚀 Starting Backend (Spring Boot)..."
cd "$SCRIPT_DIR"
./mvnw spring-boot:run > /tmp/pocketaccount-backend.log 2>&1 &
BACKEND_PID=$!
echo "   Backend PID: $BACKEND_PID"

# Wait for backend to be ready
echo "⏳ Waiting for backend to start..."
for i in {1..30}; do
  if curl -s http://localhost:8080/api/v1/documents > /dev/null 2>&1; then
    echo "✓ Backend is ready at http://localhost:8080"
    break
  fi
  sleep 1
done

# Start Frontend
echo ""
echo "🚀 Starting Frontend (Svelte)..."
cd "$SCRIPT_DIR/test-ui"
npm run dev > /tmp/pocketaccount-frontend.log 2>&1 &
FRONTEND_PID=$!
echo "   Frontend PID: $FRONTEND_PID"

# Wait for frontend to be ready
echo "⏳ Waiting for frontend to start..."
for i in {1..30}; do
  if curl -s http://localhost:5173 > /dev/null 2>&1; then
    echo "✓ Frontend is ready at http://localhost:5173"
    break
  fi
  sleep 1
done

echo ""
echo "════════════════════════════════════════════════════════════════"
echo "✅ PocketAccount is ready!"
echo "════════════════════════════════════════════════════════════════"
echo ""
echo "📱 Frontend:      http://localhost:5173"
echo "🔌 Backend API:   http://localhost:8080/api/v1"
echo "📚 Swagger Docs:  http://localhost:8080/swagger-ui.html"
echo ""
echo "📋 Logs:"
echo "   Backend:  tail -f /tmp/pocketaccount-backend.log"
echo "   Frontend: tail -f /tmp/pocketaccount-frontend.log"
echo ""
echo "🛑 To stop: kill $BACKEND_PID $FRONTEND_PID"
echo "════════════════════════════════════════════════════════════════"
echo ""

# Keep script running
wait $BACKEND_PID $FRONTEND_PID
