package dev.kavu.gameapi.statistic;

import org.bukkit.event.Event;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.plugin.Plugin;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.*;
import java.util.function.Function;

public class StatisticRegistry<T extends Number> {

    // Fields
    private final Statistic<T> statistic;
    private final HashMap<UUID, T> members = new HashMap<>();


    // Constructors
    public StatisticRegistry(Statistic<T> statistic, Plugin plugin){
        this.statistic = statistic;
        for(Trigger<?> t : statistic.getTriggers()){
            plugin.getServer().getPluginManager().registerEvent(t.getEventClass(), new Listener() { }, EventPriority.NORMAL, (listener, event) -> onEventCall(event), plugin);
        }
    }

    public StatisticRegistry(Statistic<T> statistic, Collection<UUID> initialMembers, Plugin plugin){
        this.statistic = statistic;
        for(UUID member : initialMembers){
            addMember(member);
        }
        for(Trigger<?> t : statistic.getTriggers()){
            plugin.getServer().getPluginManager().registerEvent(t.getEventClass(), new Listener() { }, EventPriority.NORMAL, (listener, event) -> onEventCall(event), plugin);
        }
    }

    // Getters
    public Statistic<T> getStatistic() {
        return statistic;
    }

    public HashMap<UUID, T> getMembers() {
        return members;
    }

    // Functionality
    public void addMember(UUID uuid){
        members.putIfAbsent(uuid, statistic.getDefault());
    }

    public void trigger(UUID uuid){
        if(!checkConditions()) return;
        execFor(uuid, statistic::onTrigger);
    }

    public boolean exec(Function<T, T> function) {

        if(members.isEmpty()) return false;

        boolean result = true;

        for (Map.Entry<UUID, T> e : members.entrySet()){
            result = execFor(e.getKey(), function) && result;
        }

        return result;
    }

    public boolean execFor(UUID member, Function<T, T> function) {

        if(members.isEmpty()) return false;

        if(function == null) throw new NullPointerException();

        return members.replace(member, function.apply(members.get(member))) == null;
    }

    private boolean checkConditions() throws IllegalArgumentException{
        boolean result = true;
        boolean alternativeResult = false;

        for(Method method : statistic.getClass().getDeclaredMethods()){
            if(method.isAnnotationPresent(Condition.class)){
                Condition condition = method.getAnnotation(Condition.class);

                method.setAccessible(true);

                try {
                    if(!condition.alternative()) {
                        result = ((boolean) method.invoke(statistic) == !condition.negate()) && result;
                    } else {
                        alternativeResult = ((boolean) method.invoke(statistic) && !condition.negate()) || alternativeResult;
                    }
                } catch (IllegalAccessException | InvocationTargetException ignored) {
                    return false;
                }
            }
        }
        return result || alternativeResult;
    }

    private void onEventCall(Event event){
        for(Trigger<?> trigger : statistic.getTriggers()){
            Class<? extends Event> clazz = trigger.getEventClass();
            if(clazz.equals(event.getClass())){
                System.out.println("Found one");
                UUID uuid = trigger.compute(clazz.cast(event));
                execFor(uuid, statistic::onTrigger);
            }
        }
    }
}