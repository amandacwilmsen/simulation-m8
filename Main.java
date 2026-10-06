
import java.io.*;
import java.math.BigInteger;
import java.nio.file.*;
import java.util.*;

public class Main {
    enum Tipo { CHEGADA, SAIDA }
    record Evento(double tempo, long ordem, Tipo tipo, String fila, String destino) implements Comparable<Evento> {
        public int compareTo(Evento o) { int c=Double.compare(tempo,o.tempo); return c!=0?c:Long.compare(ordem,o.ordem); }
    }
    static class Destino { String target; double probability; Destino(String t,double p){target=t;probability=p;} }
    static class FilaDef {
        String nome; int servers=1, capacity=-1; Double minArrival,maxArrival; double minService,maxService;
        List<Destino> destinos=new ArrayList<>();
    }
    static class FilaState {
        FilaDef def; int pop=0; long perdas=0; double ultimoTempo=0; ArrayList<Double> tempos=new ArrayList<>();
        FilaState(FilaDef d){def=d; tempos.add(0.0);}
        void garante(int n){while(tempos.size()<=n) tempos.add(0.0);}
        void acumula(double t){garante(pop); tempos.set(pop,tempos.get(pop)+(t-ultimoTempo)); ultimoTempo=t;}
    }
    static class LCG {
        static final BigInteger A=BigInteger.valueOf(25214903917L), C=BigInteger.valueOf(11L), M=BigInteger.valueOf(281474976710656L);
        BigInteger x; long count=0, limit;
        LCG(long limit,long seed){this.limit=limit; x=BigInteger.valueOf(seed);}
        boolean hasNext(){return count<limit;}
        double next(){if(!hasNext())throw new NoSuchElementException(); x=A.multiply(x).add(C).mod(M); count++; return x.doubleValue()/M.doubleValue();}
        double uniform(double min,double max){return min+(max-min)*next();}
    }
    static class Model {
        LinkedHashMap<String,FilaDef> queues=new LinkedHashMap<>(); LinkedHashMap<String,Double> arrivals=new LinkedHashMap<>();
        List<Long> seeds=new ArrayList<>(); long rndnumbersPerSeed=100000;
    }
    static class Simulador {
        Model m; LCG rnd; LinkedHashMap<String,FilaState> fs=new LinkedHashMap<>(); PriorityQueue<Evento> eventos=new PriorityQueue<>();
        double tempo=0; long ordem=0;
        Simulador(Model m,long seed){this.m=m; rnd=new LCG(m.rndnumbersPerSeed,seed); for(var q:m.queues.values())fs.put(q.nome,new FilaState(q)); for(var a:m.arrivals.entrySet())agenda(a.getValue(),Tipo.CHEGADA,a.getKey(),null);}
        void agenda(double t,Tipo tipo,String fila,String dest){eventos.add(new Evento(t,ordem++,tipo,fila,dest));}
        void run(){try{while(rnd.hasNext()&&!eventos.isEmpty())step();}catch(NoSuchElementException ignored){} for(var s:fs.values())s.acumula(tempo);}
        void step(){Evento e=eventos.poll(); tempo=e.tempo; FilaState q=fs.get(e.fila); q.acumula(tempo); if(e.tipo==Tipo.CHEGADA)chegada(q,true); else saida(q,e.destino);}
        void chegada(FilaState q,boolean externa){
            if(q.def.capacity>=0 && q.pop>=q.def.capacity) q.perdas++;
            else {int antes=q.pop++; q.garante(q.pop); if(antes<q.def.servers) agenda(tempo+rnd.uniform(q.def.minService,q.def.maxService),Tipo.SAIDA,q.def.nome,escolheDestino(q.def));}
            if(externa && q.def.minArrival!=null) agenda(tempo+rnd.uniform(q.def.minArrival,q.def.maxArrival),Tipo.CHEGADA,q.def.nome,null);
        }
        void saida(FilaState q,String destino){
            q.pop--;
            if(q.pop>=q.def.servers) agenda(tempo+rnd.uniform(q.def.minService,q.def.maxService),Tipo.SAIDA,q.def.nome,escolheDestino(q.def));
            if(destino!=null){FilaState d=fs.get(destino); d.acumula(tempo); chegada(d,false);}
        }
        String escolheDestino(FilaDef q){if(q.destinos.isEmpty())return null; double u=rnd.next(),acc=0; for(var d:q.destinos){acc+=d.probability;if(u<acc)return d.target;} return null;}
        void report(PrintStream out){out.printf(Locale.US,"Random numbers used: %d/%d%n",rnd.count,rnd.limit); out.printf(Locale.US,"Global simulation time: %.6f%n%n",tempo); for(var s:fs.values()){String cap=s.def.capacity<0?"":"/"+s.def.capacity; out.printf("Queue: %s (G/G/%d%s)%n",s.def.nome,s.def.servers,cap); out.printf("Losses: %d%n",s.perdas); out.println("State\tAccumulated time\tProbability"); for(int i=0;i<s.tempos.size();i++){double t=s.tempos.get(i); if(t>0){out.printf(Locale.US,"%d\t%.6f\t%.6f%%%n",i,t,tempo==0?0:100*t/tempo);}} out.println();}}
    }

    static Model load(String path)throws IOException{
        List<String> ls=Files.readAllLines(Path.of(path)); Model m=new Model(); String sec="", currentQ=null; Map<String,String> net=null;
        for(String raw:ls){String line=raw.trim(); if(line.isEmpty()||line.startsWith("#")||line.equals("!PARAMETERS"))continue;
            if(!raw.startsWith(" ")&&!line.startsWith("-")){sec=line.replace(":",""); currentQ=null; continue;}
            if(sec.equals("arrivals")){String[]p=kv(line);m.arrivals.put(p[0],Double.parseDouble(p[1]));}
            else if(sec.equals("queues")){
                if(raw.startsWith("   ")&&!raw.startsWith("      ")&&line.endsWith(":")){currentQ=line.substring(0,line.length()-1);FilaDef q=new FilaDef();q.nome=currentQ;m.queues.put(currentQ,q);}
                else if(currentQ!=null){String[]p=kv(line);FilaDef q=m.queues.get(currentQ);switch(p[0]){case"servers"->q.servers=Integer.parseInt(p[1]);case"capacity"->q.capacity=Integer.parseInt(p[1]);case"minArrival"->q.minArrival=Double.parseDouble(p[1]);case"maxArrival"->q.maxArrival=Double.parseDouble(p[1]);case"minService"->q.minService=Double.parseDouble(p[1]);case"maxService"->q.maxService=Double.parseDouble(p[1]);}}
            } else if(sec.equals("network")){
                if(line.startsWith("-")){if(net!=null)addNet(m,net);net=new HashMap<>();line=line.substring(1).trim();if(!line.isEmpty()){String[]p=kv(line);net.put(p[0],p[1]);}}
                else {String[]p=kv(line);net.put(p[0],p[1]);}
            } else if(sec.equals("seeds")){if(line.startsWith("-"))m.seeds.add(Long.parseLong(line.substring(1).trim()));}
            else if(sec.equals("rndnumbersPerSeed")){}
            if(line.startsWith("rndnumbersPerSeed:")){m.rndnumbersPerSeed=Long.parseLong(kv(line)[1]);sec="";}
        }
        if(net!=null)addNet(m,net); if(m.seeds.isEmpty())m.seeds.add(1L); return m;
    }
    static void addNet(Model m,Map<String,String> n){if(n.get("source")!=null)m.queues.get(n.get("source")).destinos.add(new Destino(n.get("target"),Double.parseDouble(n.get("probability"))));}
    static String[] kv(String s){int i=s.indexOf(':');return new String[]{s.substring(0,i).trim(),s.substring(i+1).trim()};}
    public static void main(String[]args)throws Exception{
        if(args.length<2||!args[0].equals("run")){System.out.println("usage:\n  java -jar simulador-filas.jar run <modelfilename.yml>");return;}
        Model m=load(args[1]); int n=1; for(long seed:m.seeds){System.out.println("Simulation: #"+n++ + " (seed "+seed+")");Simulador s=new Simulador(m,seed);s.run();s.report(System.out);}
    }
}
