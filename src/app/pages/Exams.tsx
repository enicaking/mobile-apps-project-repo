import { useState } from "react";
import { Plus, Trophy, Calendar, Trash2, Edit2, Award } from "lucide-react";
import { Button } from "@/app/components/ui/button";
import { Card, CardContent, CardHeader, CardTitle } from "@/app/components/ui/card";
import { Dialog, DialogContent, DialogHeader, DialogTitle, DialogTrigger, DialogFooter } from "@/app/components/ui/dialog";
import { Input } from "@/app/components/ui/input";
import { Label } from "@/app/components/ui/label";
import { useStudy } from "@/app/context/StudyContext";

export function Exams() {
  const { exams, friends, sessions, addExam, updateExam, deleteExam } = useStudy();
  const [isAddOpen, setIsAddOpen] = useState(false);
  const [examName, setExamName] = useState("");
  const [examDate, setExamDate] = useState("");

  const handleAddExam = () => {
    if (examName && examDate) {
      const exam = {
        id: `exam-${Date.now()}`,
        name: examName,
        date: examDate,
        isActive: true,
      };
      addExam(exam);
      setExamName("");
      setExamDate("");
      setIsAddOpen(false);
    }
  };

  const handleToggleActive = (examId: string, isActive: boolean) => {
    updateExam(examId, { isActive: !isActive });
  };

  const handleDeleteExam = (examId: string) => {
    if (confirm("Are you sure you want to delete this exam?")) {
      deleteExam(examId);
    }
  };

  const getLeaderboard = (examId: string) => {
    const examSessions = sessions.filter((s) => s.examId === examId);
    
    const friendStats = friends.map((friend) => {
      const friendSessions = examSessions.filter((s) => s.friendId === friend.id);
      const totalMinutes = Math.floor(
        friendSessions.reduce((sum, s) => sum + s.duration, 0) / 60
      );
      
      return {
        ...friend,
        totalMinutes,
        sessionCount: friendSessions.length,
      };
    });

    return friendStats
      .filter((f) => f.totalMinutes > 0)
      .sort((a, b) => b.totalMinutes - a.totalMinutes);
  };

  return (
    <div className="max-w-4xl mx-auto space-y-4">
      {/* Header */}
      <div className="flex items-center justify-between gap-3">
        <div className="min-w-0">
          <h2 className="text-xl font-bold text-gray-900 truncate">Exams</h2>
          <p className="text-xs text-gray-600">Track study time for your exams</p>
        </div>
        <Dialog open={isAddOpen} onOpenChange={setIsAddOpen}>
          <DialogTrigger asChild>
            <Button className="bg-indigo-600 hover:bg-indigo-700 flex-shrink-0 h-10 px-3">
              <Plus className="w-4 h-4 sm:mr-2" />
              <span className="hidden sm:inline">Add Exam</span>
            </Button>
          </DialogTrigger>
          <DialogContent>
            <DialogHeader>
              <DialogTitle>Add New Exam</DialogTitle>
            </DialogHeader>
            <div className="space-y-4 py-4">
              <div className="space-y-2">
                <Label htmlFor="exam-name">Exam Name</Label>
                <Input
                  id="exam-name"
                  placeholder="e.g., Math Final"
                  value={examName}
                  onChange={(e) => setExamName(e.target.value)}
                />
              </div>
              <div className="space-y-2">
                <Label htmlFor="exam-date">Exam Date</Label>
                <Input
                  id="exam-date"
                  type="date"
                  value={examDate}
                  onChange={(e) => setExamDate(e.target.value)}
                />
              </div>
            </div>
            <DialogFooter>
              <Button variant="outline" onClick={() => setIsAddOpen(false)}>
                Cancel
              </Button>
              <Button onClick={handleAddExam} className="bg-indigo-600 hover:bg-indigo-700">
                Add Exam
              </Button>
            </DialogFooter>
          </DialogContent>
        </Dialog>
      </div>

      {/* Exams List */}
      {exams.length === 0 ? (
        <Card>
          <CardContent className="py-12 text-center text-gray-500">
            <Trophy className="w-12 h-12 mx-auto mb-3 opacity-50" />
            <p className="text-sm">No exams yet</p>
            <p className="text-xs mt-1">Add an exam to start tracking study time!</p>
          </CardContent>
        </Card>
      ) : (
        <div className="space-y-3">
          {exams.map((exam) => {
            const leaderboard = getLeaderboard(exam.id);
            const totalSessions = sessions.filter((s) => s.examId === exam.id).length;
            const examDate = new Date(exam.date);
            const isUpcoming = examDate > new Date();
            const daysUntil = Math.ceil((examDate.getTime() - Date.now()) / (1000 * 60 * 60 * 24));

            return (
              <Card key={exam.id} className={exam.isActive ? "" : "opacity-60"}>
                <CardHeader className="pb-3">
                  <div className="flex items-start justify-between gap-2">
                    <div className="flex-1 min-w-0">
                      <div className="flex items-center gap-2 flex-wrap">
                        <CardTitle className="text-base sm:text-xl truncate">{exam.name}</CardTitle>
                        {!exam.isActive && (
                          <span className="text-xs bg-gray-200 text-gray-700 px-2 py-1 rounded">
                            Archived
                          </span>
                        )}
                      </div>
                      <div className="flex items-center gap-1 text-xs sm:text-sm text-gray-600 mt-1">
                        <Calendar className="w-3 h-3 sm:w-4 sm:h-4" />
                        <span>{examDate.toLocaleDateString()}</span>
                        {isUpcoming && (
                          <span className="text-indigo-600 ml-2">
                            ({daysUntil} {daysUntil === 1 ? "day" : "days"} left)
                          </span>
                        )}
                      </div>
                    </div>
                    <div className="flex gap-1 flex-shrink-0">
                      <Button
                        size="sm"
                        variant="outline"
                        onClick={() => handleToggleActive(exam.id, exam.isActive)}
                        className="h-8 w-8 p-0"
                      >
                        <Edit2 className="w-3 h-3" />
                      </Button>
                      <Button
                        size="sm"
                        variant="outline"
                        onClick={() => handleDeleteExam(exam.id)}
                        className="h-8 w-8 p-0"
                      >
                        <Trash2 className="w-3 h-3" />
                      </Button>
                    </div>
                  </div>
                </CardHeader>
                <CardContent>
                  <div className="space-y-3">
                    <div className="text-xs sm:text-sm text-gray-600">
                      {totalSessions} study {totalSessions === 1 ? "session" : "sessions"}
                    </div>

                    {/* Leaderboard */}
                    {leaderboard.length > 0 ? (
                      <div className="space-y-2">
                        <div className="flex items-center gap-2 text-xs sm:text-sm font-medium text-gray-700 mb-2">
                          <Award className="w-4 h-4" />
                          <span>Rankings</span>
                        </div>
                        {leaderboard.map((friend, index) => (
                          <div
                            key={friend.id}
                            className={`flex items-center justify-between p-2 sm:p-3 rounded-lg ${
                              index === 0
                                ? "bg-yellow-50 border border-yellow-200"
                                : index === 1
                                ? "bg-gray-50 border border-gray-200"
                                : index === 2
                                ? "bg-orange-50 border border-orange-200"
                                : "bg-gray-50"
                            }`}
                          >
                            <div className="flex items-center gap-2 flex-1 min-w-0">
                              <div className="w-6 h-6 sm:w-8 sm:h-8 rounded-full flex items-center justify-center font-bold text-xs sm:text-sm flex-shrink-0">
                                {index === 0 && "🥇"}
                                {index === 1 && "🥈"}
                                {index === 2 && "🥉"}
                                {index > 2 && (
                                  <span className="text-gray-500">{index + 1}</span>
                                )}
                              </div>
                              <div
                                className="w-6 h-6 sm:w-8 sm:h-8 rounded-full flex items-center justify-center text-white font-semibold text-xs sm:text-sm flex-shrink-0"
                                style={{ backgroundColor: friend.color }}
                              >
                                {friend.name.charAt(0)}
                              </div>
                              <div className="min-w-0">
                                <div className="font-medium text-gray-900 text-xs sm:text-sm truncate">{friend.name}</div>
                                <div className="text-xs text-gray-500">
                                  {friend.sessionCount} {friend.sessionCount === 1 ? "session" : "sessions"}
                                </div>
                              </div>
                            </div>
                            <div className="text-right flex-shrink-0">
                              <div className="text-sm sm:text-lg font-bold text-indigo-600">
                                {Math.floor(friend.totalMinutes / 60)}h {friend.totalMinutes % 60}m
                              </div>
                            </div>
                          </div>
                        ))}
                      </div>
                    ) : (
                      <div className="text-center py-4 text-xs sm:text-sm text-gray-500">
                        No study sessions for this exam yet
                      </div>
                    )}
                  </div>
                </CardContent>
              </Card>
            );
          })}
        </div>
      )}
    </div>
  );
}