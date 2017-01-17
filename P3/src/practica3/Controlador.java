package practica3;

import com.eclipsesource.json.Json;
import com.eclipsesource.json.JsonArray;
import com.eclipsesource.json.JsonObject;
import es.upv.dsic.gti_ia.core.ACLMessage;
import es.upv.dsic.gti_ia.core.AgentID;
import es.upv.dsic.gti_ia.core.SingleAgent;
import java.io.FileOutputStream;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;
import javafx.util.Pair;

/**
 *
 * @author Gregorio Carvajal Expósito
 * @author Juan José Jimenez García
 */
public class Controlador extends SingleAgent {

    public static final String SERVER_NAME = "Furud";
    public static final String AGENT_ID = "controlador";
    private static final String AGENTES_CONVERSATION_ID = "grupo-6-agentes";
    private final int MUNDO_ELEGIDO;
    private boolean fuelMundoAcabado;
    private boolean terminado;
    private EstadosEjecucion estadoActual;
    private String conversationID;
    private Map<String, AgentID> agentesMAP;
    private Heuristica heuristica;
    private BaseConocimiento bc;

    /**
     * Constructor de la clase Controlador
     *
     * @param aid El ID del agente
     * @param mundo String que indica el mundo al que se van a conectar los
     * agentes
     * @throws Exception
     * @author Juan José Jiménez García
     */
    public Controlador(AgentID aid, int mundo) throws Exception {
        super(aid);
        this.MUNDO_ELEGIDO = mundo;
    }

    /**
     * Inicialización de las variables antes de la ejecución del Controlador
     *
     * @author Juan José Jiménez García
     */
    @Override
    public void init() {
        System.out.println("Iniciando estado del Controlador...");
        this.agentesMAP = new HashMap<>();
        this.heuristica = new Heuristica();
        this.bc = BaseConocimiento.getInstance();
        this.fuelMundoAcabado = false;
        this.terminado = false;
        this.estadoActual = EstadosEjecucion.INICIAL;
        this.conversationID = "";
    }

    /**
     * Método que contiene la lógica que ejecutará el Controlador cuando se
     * inicie
     *
     * @author Juan José Jiménez García
     */
    @Override
    public void execute() {
        
        while(!terminado) {
            switch(this.estadoActual) {
                case INICIAL:
                    // Realizamos orden subscribe al servidor
                    this.suscribirse();
                    
                    // Recibimos el mensaje que debería contener el conversationID
                    try {
                        this.recibir();
                    } catch (InterruptedException ex) {
                        Logger.getLogger(Controlador.class.getName()).log(Level.SEVERE, null, ex);
                    }
                    
                    // Si hemos obtenido el conversationID, continuar
                    if(this.conversationID != "") {
                        // Obtener tamaño del mapa
                        int tamMapa = this.obtenerTamanoMapa();
                        
                        // Cargar el mapa
                        boolean resultado = bc.cargarMapa(this.MUNDO_ELEGIDO, tamMapa);
                    }
                    // Si no lo hemos obtenido, ha habido un error
                    else {
                        this.estadoActual = EstadosEjecucion.ERROR;
                    }
                    break;
                    
                case BUSCANDO:
                    break;
                case ENCONTRADO:
                    break;
                case ALCANZADO:
                    break;
                case TERMINADO:
                    this.terminado = true;
                    break;
                case ERROR:
                    break;
            }
        }
    }

    /**
     * Método que se ejecutará cuando el Controlador vaya a finalizar su
     * ejecución
     *
     * @author Juan José Jiménez García
     */
    @Override
    public void finalize() {
        
        System.out.println("Finalizando agente...");
        super.finalize();
    }

    /**
     * Método que lee una imagen de traza de un mapa y obtiene de ella el tamaño
     * del mismo
     *
     * @author Juan José Jiménez García
     */
    public int obtenerTamanoMapa() {

        return 1;
    }
    
    /**
     * Método para el procesamiento de la traza de imagen
     *
     * @author Juan José Jiménez García
     * @author Gregorio Carvajal Expósito
     * @param injson Objeto json que contiene la traza
     * @throws java.io.IOException
     */
    public void procesarTraza(JsonObject injson) throws IOException {

        try {

            JsonArray ja = injson.get("trace").asArray();
            byte data[] = new byte[ja.size()];

            for (int i = 0; i < data.length; i++) {
                data[i] = (byte) ja.get(i).asInt();
            }

            String filename = this.MUNDO_ELEGIDO + " - " + new SimpleDateFormat("yyyy-MM-dd-hh-mm").format(new Date()) + ".png";
            FileOutputStream fos = new FileOutputStream(filename);
            fos.write(data);
            fos.close();
            System.out.println("Traza guardada en el archivo " + filename);

        } catch (IOException ex) {
            System.err.println("Error procesando la traza");
            System.err.println(ex.toString());
        }
    }

    /**
     * Ejecuta el receive y actualiza las variables necesarias del controlador
     *
     * @author Gregorio Carvajal Expósito
     * @throws java.lang.InterruptedException
     */
    public void recibir() throws InterruptedException {
        ACLMessage resp = receiveACLMessage();
        JsonObject json = Json.parse(resp.getContent()).asObject();

        switch (resp.getPerformativeInt()) {
            case ACLMessage.INFORM:
                //Del servidor
                if (!resp.getConversationId().equals(AGENTES_CONVERSATION_ID))
				{
                    if (json.get("result") != null) //OK to subscribe
                    {
                        conversationID = resp.getConversationId();
                    } else //Trace
                    {
                        //PROCESAR TRAZA
                    }
                } else //De un agente
                {
                    //Respuesta al QUERY_REF pidiendo el estado
                    if (json.get("estado") != null)
					{
                        JsonObject jsonEstado = json.get("estado").asObject();
						JsonArray jsonRadar = jsonEstado.get("percepcion").asArray();

						EstadoAgente estado = new EstadoAgente(
								new int[1][1],
								new Pair<>(jsonEstado.get("i").asInt(), jsonEstado.get("j").asInt()),
								jsonEstado.get("fuelActual").asInt(),
								jsonEstado.get("crashed").asBoolean(),
								jsonEstado.get("pisandoObjetivo").asBoolean(),
								jsonEstado.get("replayWithControlador").asString(),
								TiposAgente.valueOf(jsonEstado.get("tipo").asString()),
								Acciones.valueOf(jsonEstado.get("nextAction").asString())
						);
						
						int [][] radar = new int[estado.getVisibilidad()][estado.getVisibilidad()];
						
						for (int i = 0; i < estado.getVisibilidad()*estado.getVisibilidad(); i++) {
						   radar[i / estado.getVisibilidad()][i % estado.getVisibilidad()] = jsonRadar.get(i).asInt();
						}
						
						estado.setPercepcion(radar);
						
                    } else //Respuesta al REQUEST de la accion escogida
                    {
                        //Do nothing
                    }
                }

                break;

            case ACLMessage.QUERY_REF:
                agentesMAP.put(resp.getReplyWith(), resp.getSender());
                break;

            default:
				if (resp.getConversationId().equals(AGENTES_CONVERSATION_ID)) //Recibido de un Agente
				{
					if (json.get("details").asString().contains("BAD_ENERGY"))
					{
						fuelMundoAcabado = true;
						break;
					}
					else //Recibido directamente del Server
					{
						System.err.println("ERROR: Un agente ha recibido " + json.get("details").asString());
						logout();
					}
				}
				else
					System.err.println("ERROR: El controlador ha recibido " + json.get("details").asString());
                
				//Hacer algo para detener la ejecucion de los agentes
                break;
        }
    }

    /**
     * Informa (send) Solo del al agente seleccionado su siguiente accion
     *
     * @author Gregorio Carvajal Expósito
     * @param agenteElegido Clase en la que tenemos el agente elegido y la
     * accion siguiente
     */
    public void asignarAccion(EstadoAgente agenteElegido) {
        JsonObject json = new JsonObject();
        ACLMessage msg = new ACLMessage(ACLMessage.REQUEST);

        json.add("command", agenteElegido.getNextAction().toString());

        msg.setSender(this.getAid());
        msg.setContent(json.toString());
        msg.setInReplyTo(agenteElegido.getReplyWithControlador());
        msg.setReceiver(agentesMAP.get(agenteElegido.getReplyWithControlador()));
        msg.setConversationId(AGENTES_CONVERSATION_ID);

        send(msg);
    }

    /**
     * Manda (send) a todos los agentes el ConversationID necesario para
     * comunicarse con el Servidor
     *
     * @author Gregorio Carvajal Expósito
     */
    public void compartirConversationID() {
        JsonObject json = new JsonObject();
        json.add("serverID", conversationID);

        for (Map.Entry<String, AgentID> agente : agentesMAP.entrySet()) {
            ACLMessage msg = new ACLMessage(ACLMessage.INFORM);

            msg.setSender(this.getAid());
            msg.setContent(json.toString());
            msg.setInReplyTo(agente.getKey());
            msg.setReceiver(agente.getValue());
            msg.setConversationId(AGENTES_CONVERSATION_ID);

            send(msg);
        }
    }

    /**
     * Solicita (send) a todos los agentes su estado
     *
     * @author Gregorio Carvajal Expósito
     */
    public void pedirEstadoAgente() {
        JsonObject json = new JsonObject();
        json.add("query", "estado");

        for (Map.Entry<String, AgentID> agente : agentesMAP.entrySet()) {
            ACLMessage msg = new ACLMessage(ACLMessage.QUERY_REF);

            msg.setSender(this.getAid());
            msg.setContent(json.toString());
            msg.setInReplyTo(agente.getKey());
            msg.setReceiver(agente.getValue());
            msg.setConversationId(AGENTES_CONVERSATION_ID);

            send(msg);
        }
    }

    /**
     * Realiza el SUBSCRIBE (send) al Servidor JsonObject json = new
     * JsonObject();
     *
     * @author Gregorio Carvajal Expósito
     */
    public void suscribirse() {
        ACLMessage subs = new ACLMessage(ACLMessage.SUBSCRIBE);
        JsonObject json = new JsonObject();

        subs.setSender(this.getAid());
        subs.setReceiver(new AgentID(SERVER_NAME));
        json.add("world", "map" + MUNDO_ELEGIDO);
        subs.setContent(json.toString());

        send(subs);
    }

    /**
     * Realiza el logout (send) al Servidor
     *
     * @author Gregorio Carvajal Expósito
     */
    public void logout() {
        ACLMessage cancel = new ACLMessage(ACLMessage.CANCEL);

        cancel.setSender(this.getAid());
        cancel.setReceiver(new AgentID(SERVER_NAME));

        send(cancel);
    }
}
