import { useState } from "react";
import { Calendar, Clock, Trophy } from "lucide-react";
import { Card, CardContent, CardHeader, CardTitle } from "@/app/components/ui/card";
import { Select, SelectContent, SelectItem, SelectTrigger, SelectValue } from "@/app/components/ui/select";
import { useStudy } from "@/app/context/StudyContext";

export function History() {
  const { sessions, friends, exams } = useStudy();
  const [selectedFriend, setSelectedFriend] = useState<string>("all");
  const [selectedExam, setSelectedExam] = useState<string>("all");

  const filteredSessions = sessions
    .filter((s) => selectedFriend === "all" || s.friendId === selectedFriend)
    .filter((s) => selectedExam === "all" || s.examId === selectedExam)
    .sort((a, b) => b.startTime - a.startTime);

  const formatDuration = (seconds: number) => {
    const hours = Math.floor(seconds / 3600);
    const minutes = Math.floor((seconds % 3600) / 60);
    if (hours > 0) {
      return `${hours}h ${minutes}m`;
    }
    return `${minutes}m`;
  };

  const formatDate = (timestamp: number) => {
    const date = new Date(timestamp);
    const today = new Date();
    const yesterday = new Date(today);
    yesterday.setDate(yesterday.getDate() - 1);

    if (date.toDateString() === today.toDateString()) {
      return `Today, ${date.toLocaleTimeString([], { hour: "2-digit", minute: "2-digit" })}`;
    } else if (date.toDateString() === yesterday.toDateString()) {
      return `Yesterday, ${date.toLocaleTimeString([], { hour: "2-digit", minute: "2-digit" })}`;
    }
    return date.toLocaleDateString([], {
      month: "short",
      day: "numeric",
      hour: "2-digit",
      minute: "2-digit",
    });
  };

  const totalMinutes = Math.floor(
    filteredSessions.reduce((sum, s) => sum + s.duration, 0) / 60
  );

  return (
    <div className="max-w-4xl mx-auto space-y-4">
      <Card>
        <CardHeader className="pb-3">
          <CardTitle className="text-lg">Study History</CardTitle>
        </CardHeader>
        <CardContent className="space-y-4">
          {/* Filters */}
          <div className="grid grid-cols-1 gap-3">
            <div className="space-y-2">
              <label className="text-xs font-medium text-gray-700">Filter by Person</label>
              <Select value={selectedFriend} onValueChange={setSelectedFriend}>
                <SelectTrigger className="h-11">
                  <SelectValue />
                </SelectTrigger>
                <SelectContent>
                  <SelectItem value="all">All Friends</SelectItem>
                  {friends.map((friend) => (
                    <SelectItem key={friend.id} value={friend.id}>
                      {friend.name}
                    </SelectItem>
                  ))}
                </SelectContent>
              </Select>
            </div>

            <div className="space-y-2">
              <label className="text-xs font-medium text-gray-700">Filter by Exam</label>
              <Select value={selectedExam} onValueChange={setSelectedExam}>
                <SelectTrigger className="h-11">
                  <SelectValue />
                </SelectTrigger>
                <SelectContent>
                  <SelectItem value="all">All Exams</SelectItem>
                  {exams.map((exam) => (
                    <SelectItem key={exam.id} value={exam.id}>
                      {exam.name}
                    </SelectItem>
                  ))}
                </SelectContent>
              </Select>
            </div>
          </div>

          {/* Total Summary */}
          <div className="bg-indigo-50 rounded-lg p-3 flex items-center justify-between">
            <span className="text-xs sm:text-sm font-medium text-gray-700">Total Study Time</span>
            <span className="text-xl sm:text-2xl font-bold text-indigo-600">
              {Math.floor(totalMinutes / 60)}h {totalMinutes % 60}m
            </span>
          </div>
        </CardContent>
      </Card>

      {/* Sessions List */}
      <div className="space-y-2">
        {filteredSessions.length === 0 ? (
          <Card>
            <CardContent className="py-12 text-center text-gray-500">
              <Clock className="w-12 h-12 mx-auto mb-3 opacity-50" />
              <p className="text-sm">No study sessions yet</p>
              <p className="text-xs mt-1">Start a timer to track your study time!</p>
            </CardContent>
          </Card>
        ) : (
          filteredSessions.map((session) => {
            const friend = friends.find((f) => f.id === session.friendId);
            const exam = exams.find((e) => e.id === session.examId);
            
            return (
              <Card key={session.id} className="hover:shadow-md transition-shadow">
                <CardContent className="py-3">
                  <div className="flex items-center justify-between gap-3">
                    <div className="flex items-center gap-2 flex-1 min-w-0">
                      <div
                        className="w-10 h-10 rounded-full flex items-center justify-center text-white font-semibold flex-shrink-0"
                        style={{ backgroundColor: friend?.color }}
                      >
                        {friend?.name?.charAt(0) || "?"}
                      </div>
                      <div className="flex-1 min-w-0">
                        <div className="font-medium text-gray-900 text-sm truncate">{friend?.name}</div>
                        <div className="flex flex-col gap-1 text-xs text-gray-600 mt-1">
                          <div className="flex items-center gap-1">
                            <Calendar className="w-3 h-3 flex-shrink-0" />
                            <span className="truncate">{formatDate(session.startTime)}</span>
                          </div>
                          {exam && (
                            <div className="flex items-center gap-1">
                              <Trophy className="w-3 h-3 flex-shrink-0" />
                              <span className="truncate">{exam.name}</span>
                            </div>
                          )}
                        </div>
                      </div>
                    </div>
                    <div className="text-right flex-shrink-0">
                      <div className="text-lg sm:text-2xl font-bold text-indigo-600">
                        {formatDuration(session.duration)}
                      </div>
                      <div className="text-xs text-gray-500">Duration</div>
                    </div>
                  </div>
                </CardContent>
              </Card>
            );
          })
        )}
      </div>
    </div>
  );
}