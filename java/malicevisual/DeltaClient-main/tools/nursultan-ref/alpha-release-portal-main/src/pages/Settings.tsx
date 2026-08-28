import { Button } from "@/components/ui/button";
import { Input } from "@/components/ui/input";
import { Switch } from "@/components/ui/switch";
import { Folder, RefreshCw, Edit } from "lucide-react";

const Settings = () => {
  return (
    <div className="p-8 ml-16">
      <div className="max-w-6xl mx-auto">
        <div className="grid grid-cols-1 md:grid-cols-2 gap-8">
          <div className="glass-panel p-6">
            <h2 className="text-xl font-semibold mb-4">Оперативная память</h2>
            <p className="text-sm text-muted-foreground mb-4">
              Вы можете указать нужное для вас количество оперативной памяти, которое будет выделено для клиента
            </p>
            <div className="space-y-4">
              <div className="flex items-center gap-4">
                <div className="flex-1">
                  <div className="h-2 bg-primary/20 rounded-full overflow-hidden">
                    <div className="h-full w-3/4 bg-primary rounded-full" />
                  </div>
                </div>
                <Input type="number" value="2560" className="w-24" />
                <span className="text-sm text-muted-foreground">МБ</span>
              </div>
            </div>
          </div>
          
          <div className="glass-panel p-6">
            <h2 className="text-xl font-semibold mb-4">Расположение</h2>
            <p className="text-sm text-muted-foreground mb-4">
              Место в системе, где будут храниться все необходимые файлы для работы лаунчера и клиентов
            </p>
            <div className="flex items-center gap-2">
              <Input value="C:/Nursultan" readOnly />
              <Button variant="secondary" size="icon">
                <RefreshCw size={18} />
              </Button>
              <Button variant="secondary" size="icon">
                <Folder size={18} />
              </Button>
              <Button variant="secondary" size="icon">
                <Edit size={18} />
              </Button>
            </div>
          </div>
          
          <div className="glass-panel p-6">
            <h2 className="text-xl font-semibold mb-4">Окно игры</h2>
            <p className="text-sm text-muted-foreground mb-4">
              Вы можете указать свой желаемый размер окна, который будет установлен по умолчанию при запуске клиента
            </p>
            <div className="grid grid-cols-2 gap-4">
              <div>
                <label className="text-sm text-muted-foreground">Ширина</label>
                <Input type="number" value="925" />
              </div>
              <div>
                <label className="text-sm text-muted-foreground">Высота</label>
                <Input type="number" value="530" />
              </div>
            </div>
          </div>
          
          <div className="glass-panel p-6">
            <h2 className="text-xl font-semibold mb-4">Другое</h2>
            <div className="space-y-4">
              <div className="flex items-center justify-between">
                <span>Полноэкранный режим</span>
                <Switch />
              </div>
              <div className="flex items-center justify-between">
                <span>Переустановить клиент</span>
                <Switch />
              </div>
              <div className="flex items-center justify-between">
                <span>Режим отладки</span>
                <Switch />
              </div>
            </div>
          </div>
        </div>
      </div>
    </div>
  );
};

export default Settings;