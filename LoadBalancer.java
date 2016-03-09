public class LoadBalancer {
	public static ILoadBalancer loadBalancerSingleton;

	public LoadBalancer(ILoadBalancer ilb){
		if(loadBalancerSingleton == null){
			loadBalancerSingleton = ilb;
		}
	}
}