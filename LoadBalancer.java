/*
** NOTE: This is merely a singleton class for
** globally accessing the load balancer - NOT
** the load balancer itself.
**
** The load balancer interface is ILoadBalancer,
** and the example load balancer is actually
** MyLoadBalancer
*/

public class LoadBalancer {
	public static ILoadBalancer loadBalancerSingleton;

	public LoadBalancer(ILoadBalancer ilb){
		if(loadBalancerSingleton == null){
			loadBalancerSingleton = ilb;
		}
	}
}