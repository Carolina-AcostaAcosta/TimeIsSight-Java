#!/bin/bash

# ==========================================
# CONFIGURATION
# ==========================================
NTFY_URL="ntfy.sh/TimeIsSight-TFG-Carolina-ULL"
MACHINE=$(hostname)
JAR_FILE="target/TimeIsSight-1.0-SNAPSHOT.jar"
JSON_FILE="P15000_D365_20260629_232722.json"
TIME_LIMIT=90
TOTAL_SIMULATIONS=10
LOG_FILE="executions_${MACHINE}_$(date +%Y%m%d_%H%M%S).log"

# ==========================================
# FAIL-SAFE: Signal handling
# ==========================================
# If the script is cancelled (Ctrl+C, killed, shutdown), send an alert immediately.
trap 'curl -d "🚨 ALARM: The simulation batch on $MACHINE was abruptly interrupted or cancelled." $NTFY_URL; exit 1' SIGINT SIGTERM

# ==========================================
# VALIDATIONS
# ==========================================
if [ ! -f "$JAR_FILE" ]; then
    echo "Error: File $JAR_FILE not found. Please compile first." | tee -a "$LOG_FILE"
    curl -d "⚠️ Error: JAR not found on $MACHINE. Cancelling batch." $NTFY_URL
    exit 1
fi

# ==========================================
# MAIN LOOP
# ==========================================
curl -d "🚀 START: Launching batch of $TOTAL_SIMULATIONS simulations on $MACHINE." $NTFY_URL
echo "Starting simulations. Log saved to: $LOG_FILE" > "$LOG_FILE"

for i in $(seq 1 $TOTAL_SIMULATIONS); do
    echo "--- Starting simulation [$i/$TOTAL_SIMULATIONS] at $(date) ---" >> "$LOG_FILE"
    
    # Execute the JAR. '>> "$LOG_FILE" 2>&1' saves all outputs and errors to the file.
    java -jar "$JAR_FILE" -e "$JSON_FILE" -t "$TIME_LIMIT" >> "$LOG_FILE" 2>&1
    STATUS=$?
    
    if [ $STATUS -eq 0 ]; then
        curl -d "✅ [$i/$TOTAL_SIMULATIONS] Success on $MACHINE for $JSON_FILE." $NTFY_URL
    else
        curl -d "❌ [$i/$TOTAL_SIMULATIONS] Error on $MACHINE. Check the log: $LOG_FILE" $NTFY_URL
        # Optional: If you want the script to stop when a simulation fails, uncomment the next line:
        # exit 1 
    fi
    
    # Brief 2-second pause between executions to free up memory/processes
    sleep 2
done

curl -d "🏁 FINISH: Batch of $TOTAL_SIMULATIONS simulations completed on $MACHINE." $NTFY_URL
