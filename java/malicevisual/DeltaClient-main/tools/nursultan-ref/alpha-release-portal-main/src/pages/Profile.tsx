import { Button } from "@/components/ui/button";
import { User } from "lucide-react";

const Profile = () => {
  return (
    <div className="p-8 ml-16">
      <div className="max-w-6xl mx-auto">
        <div className="grid grid-cols-1 md:grid-cols-2 gap-8">
          <div className="glass-panel p-6">
            <h2 className="text-xl font-semibold mb-6">Аватар</h2>
            <div className="flex flex-col items-center gap-4">
              <div className="w-32 h-32 rounded-full bg-secondary flex items-center justify-center">
                <User size={48} className="text-muted-foreground" />
              </div>
              <Button variant="secondary">Загрузить/Изменить аватар</Button>
            </div>
          </div>
          
          <div className="glass-panel p-6">
            <h2 className="text-xl font-semibold mb-6">Профиль</h2>
            <div className="space-y-4">
              <div className="flex justify-between">
                <span className="text-muted-foreground">UID</span>
                <span>27515</span>
              </div>
              <div className="flex justify-between">
                <span className="text-muted-foreground">Логин</span>
                <span>Skyviper</span>
              </div>
              <div className="flex justify-between">
                <span className="text-muted-foreground">Группа</span>
                <span>Пользователь</span>
              </div>
              <div className="flex justify-between">
                <span className="text-muted-foreground">Клиент куплен до</span>
                <span>28.04.2037 00:00</span>
              </div>
            </div>
          </div>
        </div>
        
        <div className="mt-8 glass-panel p-6">
          <h2 className="text-xl font-semibold mb-6">Полезное</h2>
          <div className="grid grid-cols-1 md:grid-cols-3 gap-4">
            <Button variant="secondary" className="w-full">ЛИЧНЫЙ КАБИНЕТ</Button>
            <Button variant="secondary" className="w-full">ПРОДЛИТЬ ПОДПИСКУ</Button>
            <Button variant="secondary" className="w-full">СБРОСИТЬ HWID</Button>
          </div>
          <Button variant="secondary" className="w-full mt-4">АКТИВАЦИЯ КЛЮЧА</Button>
        </div>
      </div>
    </div>
  );
};

export default Profile;