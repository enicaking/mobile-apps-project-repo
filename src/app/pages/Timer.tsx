import { useState, useEffect, useRef } from "react";
import { Play, Pause, Square } from "lucide-react";
import { Button } from "@/app/components/ui/button";
import { Card, CardContent, CardHeader, CardTitle } from "@/app/components/ui/card";
import { Select, SelectContent, SelectItem, SelectTrigger, SelectValue } from "@/app/components/ui/select";
import { useStudy } from "@/app/context/StudyContext";

export function Timer() {
  const { friends, exams, currentUserId, addSession } = useStudy();
  const [isRunning, setIsRunning] = useState(false);
  const [elapsedTime, setElapsedTime] = useState(0);
  const [selectedExam, setSelectedExam] = useState<string>("none");
  const startTimeRef = useRef<number>(0);
  const intervalRef = useRef<number | null>(null);

  useEffect(() => {
    if (isRunning) {
      startTimeRef.current = Date.now() - elapsedTime * 1000;
      intervalRef.current = window.setInterval(() => {
        setElapsedTime(Math.floor((Date.now() - startTimeRef.current) / 1000));
      }, 100);
    } else {
      if (intervalRef.current) {
        clearInterval(intervalRef.current);
      }
    }

    return () => {
      if (intervalRef.current) {
        clearInterval(intervalRef.current);
      }
    };
  }, [isRunning, elapsedTime]);

  const formatTime = (seconds: number) => {
    const hours = Math.floor(seconds / 3600);
    const minutes = Math.floor((seconds % 3600) / 60);
    const secs = seconds % 60;
    return `${hours.toString().padStart(2, "0")}:${minutes.toString().padStart(2, "0")}:${secs.toString().padStart(2, "0")}`;
  };

  const handleStart = () => {
    setIsRunning(true);
  };

  const handlePause = () => {
    setIsRunning(false);
  };

  const handleStop = () => {
    setIsRunning(false);
    
    // Only save if there's meaningful time (at least 1 minute)
    if (elapsedTime >= 60) {
      const session = {
        id: `session-${Date.now()}`,
        friendId: currentUserId,
        startTime: startTimeRef.current,
        endTime: Date.now(),
        duration: elapsedTime,
        examId: selectedExam !== "none" ? selectedExam : undefined,
      };
      addSession(session);
    }
    
    setElapsedTime(0);
    setSelectedExam("none");
  };

  const currentFriend = friends.find((f) => f.id === currentUserId);
  const activeExams = exams.filter((e) => e.isActive);

  return (
    <div className="max-w-2xl mx-auto space-y-4">
      <Card className="shadow-lg">
        <CardHeader className="pb-3">
          <CardTitle className="text-center text-lg">Study Timer</CardTitle>
        </CardHeader>
        <CardContent className="space-y-4">
          {/* Timer Display */}
          <div className="text-center">
            <div
              className="text-5xl sm:text-7xl font-bold text-indigo-600 font-mono tracking-wider"
              style={{ fontVariantNumeric: "tabular-nums" }}
            >
              {formatTime(elapsedTime)}
            </div>
            <p className="text-xs sm:text-sm text-gray-500 mt-2">
              {isRunning ? "Studying..." : "Ready to study"}
            </p>
          </div>

          {/* Exam Selection */}
          <div className="space-y-2">
            <label className="text-xs sm:text-sm font-medium text-gray-700">
              Studying for (optional)
            </label>
            <Select value={selectedExam} onValueChange={setSelectedExam} disabled={isRunning}>
              <SelectTrigger className="h-11">
                <SelectValue placeholder="Select an exam" />
              </SelectTrigger>
              <SelectContent>
                <SelectItem value="none">No exam selected</SelectItem>
                {activeExams.map((exam) => (
                  <SelectItem key={exam.id} value={exam.id}>
                    {exam.name}
                  </SelectItem>
                ))}
              </SelectContent>
            </Select>
          </div>

          {/* Current User Info */}
          <div className="flex items-center justify-center gap-2 text-xs sm:text-sm text-gray-600">
            <div
              className="w-3 h-3 rounded-full"
              style={{ backgroundColor: currentFriend?.color }}
            />
            <span>Studying as {currentFriend?.name}</span>
          </div>

          {/* Control Buttons */}
          <div className="flex gap-2 sm:gap-3 justify-center">
            {!isRunning && elapsedTime === 0 && (
              <Button
                onClick={handleStart}
                size="lg"
                className="bg-indigo-600 hover:bg-indigo-700 text-white flex-1 sm:flex-initial sm:px-8 h-12"
              >
                <Play className="w-5 h-5 mr-2" />
                Start
              </Button>
            )}

            {isRunning && (
              <Button
                onClick={handlePause}
                size="lg"
                variant="secondary"
                className="flex-1 sm:flex-initial sm:px-8 h-12"
              >
                <Pause className="w-5 h-5 mr-2" />
                Pause
              </Button>
            )}

            {!isRunning && elapsedTime > 0 && (
              <Button
                onClick={handleStart}
                size="lg"
                className="bg-indigo-600 hover:bg-indigo-700 text-white flex-1 sm:flex-initial sm:px-8 h-12"
              >
                <Play className="w-5 h-5 mr-2" />
                Resume
              </Button>
            )}

            {elapsedTime > 0 && (
              <Button
                onClick={handleStop}
                size="lg"
                variant="destructive"
                className="flex-1 sm:flex-initial sm:px-8 h-12"
              >
                <Square className="w-5 h-5 mr-2" />
                Stop
              </Button>
            )}
          </div>

          {/* Info Message */}
          {elapsedTime > 0 && elapsedTime < 60 && !isRunning && (
            <p className="text-xs text-center text-gray-500">
              Session must be at least 1 minute to be saved
            </p>
          )}
        </CardContent>
      </Card>

      {/* Quick Stats */}
      <Card>
        <CardHeader className="pb-3">
          <CardTitle className="text-base sm:text-lg">Today's Progress</CardTitle>
        </CardHeader>
        <CardContent>
          <TodayStats />
        </CardContent>
      </Card>
    </div>
  );
}

function TodayStats() {
  const { sessions, currentUserId } = useStudy();
  
  const todayStart = new Date();
  todayStart.setHours(0, 0, 0, 0);
  
  const todaySessions = sessions.filter(
    (s) => s.friendId === currentUserId && s.startTime >= todayStart.getTime()
  );
  
  const totalMinutes = Math.floor(
    todaySessions.reduce((sum, s) => sum + s.duration, 0) / 60
  );
  
  const sessionCount = todaySessions.length;

  return (
    <div className="grid grid-cols-2 gap-4">
      <div className="text-center">
        <div className="text-2xl sm:text-3xl font-bold text-indigo-600">{sessionCount}</div>
        <div className="text-xs sm:text-sm text-gray-600">Sessions</div>
      </div>
      <div className="text-center">
        <div className="text-2xl sm:text-3xl font-bold text-indigo-600">{totalMinutes}</div>
        <div className="text-xs sm:text-sm text-gray-600">Minutes</div>
      </div>
    </div>
  );
}