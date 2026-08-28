import VersionCard from "../components/VersionCard";

const Index = () => {
  return (
    <div className="p-8 ml-16">
      <div className="max-w-6xl mx-auto">
        <h1 className="text-3xl font-bold mb-8">NURSULTAN</h1>
        
        <div className="grid grid-cols-1 md:grid-cols-2 gap-6">
          <VersionCard
            version="1.16.5"
            image="/lovable-uploads/56a1dbc0-6d39-4a3d-a3b7-db5709458e52.png"
            title="Клиент"
          />
          <VersionCard
            version="ALPHA 1.16.5"
            image="/lovable-uploads/8c67bf3c-a7cb-4f10-a5e9-e08bd756620a.png"
            title="Клиент"
          />
        </div>
      </div>
    </div>
  );
};

export default Index;