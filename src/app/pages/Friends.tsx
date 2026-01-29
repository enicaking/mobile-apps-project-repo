import { useState } from "react";
import { Users, Plus, Check, Trash2 } from "lucide-react";
import { Button } from "@/app/components/ui/button";
import { Card, CardContent, CardHeader, CardTitle } from "@/app/components/ui/card";
import { Dialog, DialogContent, DialogHeader, DialogTitle, DialogTrigger, DialogFooter } from "@/app/components/ui/dialog";
import { Input } from "@/app/components/ui/input";
import { Label } from "@/app/components/ui/label";
import { useStudy } from "@/app/context/StudyContext";

const COLORS = [
  "#3b82f6", // blue
  "#ef4444", // red
  "#10b981", // green
  "#f59e0b", // amber
  "#8b5cf6", // violet
  "#ec4899", // pink
  "#06b6d4", // cyan
  "#f97316", // orange
];

export function Friends() {
  const { friends, currentUserId, addFriend, setCurrentUser, sessions } = useStudy();
  const [isAddOpen, setIsAddOpen] = useState(false);
  const [friendName, setFriendName] = useState("");
  const [selectedColor, setSelectedColor] = useState(COLORS[1]);

  const handleAddFriend = () => {
    if (friendName.trim()) {
      const friend = {
        id: `friend-${Date.now()}`,
        name: friendName.trim(),
        color: selectedColor,
      };
      addFriend(friend);
      setFriendName("");
      setSelectedColor(COLORS[1]);
      setIsAddOpen(false);
    }
  };

  const getFriendStats = (friendId: string) => {
    const friendSessions = sessions.filter((s) => s.friendId === friendId);
    const totalMinutes = Math.floor(
      friendSessions.reduce((sum, s) => sum + s.duration, 0) / 60
    );
    return {
      sessionCount: friendSessions.length,
      totalMinutes,
    };
  };

  return (
    <div className="max-w-4xl mx-auto space-y-4">
      {/* Header */}
      <div className="flex items-center justify-between gap-3">
        <div className="min-w-0">
          <h2 className="text-xl font-bold text-gray-900 truncate">Friends</h2>
          <p className="text-xs text-gray-600">Manage your study group</p>
        </div>
        <Dialog open={isAddOpen} onOpenChange={setIsAddOpen}>
          <DialogTrigger asChild>
            <Button className="bg-indigo-600 hover:bg-indigo-700 flex-shrink-0 h-10 px-3">
              <Plus className="w-4 h-4 sm:mr-2" />
              <span className="hidden sm:inline">Add Friend</span>
            </Button>
          </DialogTrigger>
          <DialogContent>
            <DialogHeader>
              <DialogTitle>Add New Friend</DialogTitle>
            </DialogHeader>
            <div className="space-y-4 py-4">
              <div className="space-y-2">
                <Label htmlFor="friend-name">Name</Label>
                <Input
                  id="friend-name"
                  placeholder="Friend's name"
                  value={friendName}
                  onChange={(e) => setFriendName(e.target.value)}
                />
              </div>
              <div className="space-y-2">
                <Label>Color</Label>
                <div className="grid grid-cols-8 gap-2">
                  {COLORS.map((color) => (
                    <button
                      key={color}
                      className={`w-10 h-10 rounded-full transition-all ${
                        selectedColor === color
                          ? "ring-2 ring-offset-2 ring-indigo-500 scale-110"
                          : "hover:scale-105"
                      }`}
                      style={{ backgroundColor: color }}
                      onClick={() => setSelectedColor(color)}
                    >
                      {selectedColor === color && (
                        <Check className="w-5 h-5 text-white mx-auto" />
                      )}
                    </button>
                  ))}
                </div>
              </div>
            </div>
            <DialogFooter>
              <Button variant="outline" onClick={() => setIsAddOpen(false)}>
                Cancel
              </Button>
              <Button onClick={handleAddFriend} className="bg-indigo-600 hover:bg-indigo-700">
                Add Friend
              </Button>
            </DialogFooter>
          </DialogContent>
        </Dialog>
      </div>

      {/* Friends List */}
      {friends.length === 0 ? (
        <Card>
          <CardContent className="py-12 text-center text-gray-500">
            <Users className="w-12 h-12 mx-auto mb-3 opacity-50" />
            <p className="text-sm">No friends yet</p>
            <p className="text-xs mt-1">Add friends to track study time together!</p>
          </CardContent>
        </Card>
      ) : (
        <div className="grid grid-cols-1 gap-3">
          {friends.map((friend) => {
            const stats = getFriendStats(friend.id);
            const isCurrentUser = friend.id === currentUserId;

            return (
              <Card
                key={friend.id}
                className={`transition-all ${
                  isCurrentUser ? "ring-2 ring-indigo-500 shadow-lg" : "hover:shadow-md"
                }`}
              >
                <CardContent className="py-4">
                  <div className="flex items-start gap-3">
                    <div
                      className="w-12 h-12 sm:w-16 sm:h-16 rounded-full flex items-center justify-center text-white text-lg sm:text-2xl font-semibold flex-shrink-0"
                      style={{ backgroundColor: friend.color }}
                    >
                      {friend.name.charAt(0).toUpperCase()}
                    </div>
                    <div className="flex-1 min-w-0">
                      <div className="flex items-center gap-2 mb-1">
                        <h3 className="text-base sm:text-lg font-semibold text-gray-900 truncate">
                          {friend.name}
                        </h3>
                        {isCurrentUser && (
                          <span className="text-xs bg-indigo-100 text-indigo-700 px-2 py-1 rounded flex-shrink-0">
                            You
                          </span>
                        )}
                      </div>
                      <div className="grid grid-cols-2 gap-2 sm:gap-3 mt-2">
                        <div>
                          <div className="text-xl sm:text-2xl font-bold text-indigo-600">
                            {stats.sessionCount}
                          </div>
                          <div className="text-xs text-gray-600">Sessions</div>
                        </div>
                        <div>
                          <div className="text-xl sm:text-2xl font-bold text-indigo-600">
                            {Math.floor(stats.totalMinutes / 60)}h {stats.totalMinutes % 60}m
                          </div>
                          <div className="text-xs text-gray-600">Total Time</div>
                        </div>
                      </div>
                      {!isCurrentUser && (
                        <div className="mt-3">
                          <Button
                            size="sm"
                            variant="outline"
                            onClick={() => setCurrentUser(friend.id)}
                            className="text-xs h-8"
                          >
                            Switch to this user
                          </Button>
                        </div>
                      )}
                    </div>
                  </div>
                </CardContent>
              </Card>
            );
          })}
        </div>
      )}

      {/* Info Card */}
      <Card className="bg-blue-50 border-blue-200">
        <CardContent className="py-3">
          <div className="flex gap-2">
            <Users className="w-4 h-4 sm:w-5 sm:h-5 text-blue-600 flex-shrink-0 mt-0.5" />
            <div className="text-xs sm:text-sm text-blue-900">
              <p className="font-medium mb-1">About Friends</p>
              <p>
                Add your study buddies to track everyone's progress! You can switch between users to log study sessions for different people. The active user is marked with "You".
              </p>
            </div>
          </div>
        </CardContent>
      </Card>
    </div>
  );
}