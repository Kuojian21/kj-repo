package com.kj.repo.infra.demo;

import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutionException;
import java.util.stream.Collectors;

import com.facebook.ads.sdk.APIContext;
import com.facebook.ads.sdk.APIException;
import com.facebook.ads.sdk.APINode;
import com.facebook.ads.sdk.APINodeList;
import com.facebook.ads.sdk.APIRequest;
import com.facebook.ads.sdk.APIResponse;
import com.facebook.ads.sdk.Ad;
import com.facebook.ads.sdk.AdAccount;
import com.facebook.ads.sdk.AdAccount.APIRequestCreateCampaign;
import com.facebook.ads.sdk.AdCreative;
import com.facebook.ads.sdk.AdSet;
import com.facebook.ads.sdk.BatchRequest;
import com.facebook.ads.sdk.Campaign;
import com.facebook.ads.sdk.CustomAudience;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.google.common.util.concurrent.ListenableFuture;
import com.google.gson.Gson;
import com.google.gson.JsonObject;
import com.kj.repo.infra.tool.TLStream;

public class DeFacebook {

	public static void base(APIContext context, String id) throws APIException {
		System.out.println(new APIRequest<APINode>(context, id, "/", "GET").execute());
	}

	public static void allAccounts(APIContext context) throws APIException {
		APIRequest<APINode> request = new APIRequest<APINode>(context, "me", "/adaccounts", "GET");
		System.out.println(request.execute());
	}

	public static void demo(APIContext context, String accountId) throws APIException {
		AdAccount adAccount = new AdAccount(accountId, context);
		APINodeList<Campaign> campaigns = adAccount.getCampaigns()/* .requestAllFields() */.execute();

		for (Campaign c : campaigns) {
			Campaign campaign = Campaign.fetchById(c.getId(), context);
			System.out.println(campaign);
			APINodeList<AdSet> adSets = campaign.getAdSets()/* .requestAllFields() */.execute();
			for (AdSet s : adSets) {
				AdSet adSet = AdSet.fetchById(s.getId(), context);
				System.out.println(adSet);
				APINodeList<AdCreative> adCreattives = adSet.getAdCreatives()/* .requestAllFields() */.execute();
				for (AdCreative ac : adCreattives) {
					System.out.println(AdCreative.fetchById(ac.getId(), context));
				}
				APINodeList<Ad> ads = adSet.getAds().requestAllFields().execute();
				for (Ad a : ads) {
					System.out.println(Ad.fetchById(a.getId(), context));
				}
			}
		}

		APINodeList<CustomAudience> cas = adAccount.getCustomAudiences()/* .requestAllFields() */.execute();
		for (CustomAudience ca : cas) {
			System.out.println(CustomAudience.fetchById(ca.getId(), context));
		}

	}

	public static void batchCampaign(APIContext context, String campaignId) throws APIException {
		BatchRequest batch = new BatchRequest(context);
		for (int i = 0; i < 5; i++) {
			batch.addRequest(new AdAccount(campaignId, context).createCampaign().setName("TEST_" + i)
					.setObjective(Campaign.EnumObjective.VALUE_APP_INSTALLS)
					.setStatus(Campaign.EnumStatus.VALUE_ACTIVE));
		}
		List<APIResponse> response = batch.execute();
		System.out.println(response);
	}

	public static void asyncBatch(AdAccount adAccount)
			throws NoSuchMethodException, SecurityException, IllegalAccessException, IllegalArgumentException,
			InvocationTargetException, APIException, InterruptedException, ExecutionException {
		List<Object> batch = Lists.newArrayList();
		Method method = APIRequest.class.getDeclaredMethod("getBatchModeRequestInfo");
		method.setAccessible(true);
		for (int i = 0; i < 5; i++) {
			APIRequestCreateCampaign request = adAccount.createCampaign().setName("test" + i)
					.setObjective(Campaign.EnumObjective.VALUE_APP_INSTALLS)
					.setStatus(Campaign.EnumStatus.VALUE_ACTIVE);
			BatchRequest.BatchModeRequestInfo info = (BatchRequest.BatchModeRequestInfo) method.invoke(request,
					new Object[] {});
			JsonObject batchElement = new JsonObject();
			batchElement.addProperty("method", info.method);
			batchElement.addProperty("relative_url", info.relativeUrl);
			batchElement.addProperty("name", "RequestCreative" + i);
			batchElement.addProperty("body", info.body);
			batch.add(batchElement);
		}
		APIRequest.changeAsyncRequestExecutor(new com.facebook.ads.sdk.APIRequest.DefaultAsyncRequestExecutor());
		ListenableFuture<APIResponse> future = adAccount.createAsyncBatchRequest().setAdbatch(batch).executeAsyncBase();
		System.out.println(future.get());
	}

	/**
	 * https://business.facebook.com/ads/manage/customaudiences/tos?act=399613380513958
	 *
	 * @param context
	 * @param accountId
	 * @throws APIException
	 */
	public static CustomAudience createCustomAudience(APIContext context, String accountId) throws APIException {
		AdAccount adAccount = new AdAccount(accountId, context);
		CustomAudience ca = adAccount.createCustomAudience().setName("CA_AUTO_TEST")
				.setSubtype(CustomAudience.EnumSubtype.VALUE_CUSTOM)
				.setDescription("People who purchased on my website")
				.setCustomerFileSource(CustomAudience.EnumCustomerFileSource.VALUE_USER_PROVIDED_ONLY).execute();
		System.out.println(ca);
		return ca;
	}

	public static CustomAudience createLALCustomAudience(APIContext context, String accountId, String caid)
			throws APIException {
		AdAccount adAccount = new AdAccount(accountId, context);
		Map<String, Object> lookalikeSpec = Maps.newHashMap();
		lookalikeSpec.put("type", "reach");
		lookalikeSpec.put("radio", 0.01);

		Map<String, Map<String, Object>> location_spec = Maps.newHashMap();
		Map<String, Object> geo_locations = Maps.newHashMap();
		List<String> countries = Lists.newArrayList();
		countries.add("TR");
		// countries.add("BY");
		geo_locations.put("countries", countries);
		location_spec.put("geo_locations", geo_locations);
		lookalikeSpec.put("location_spec", location_spec);
		// lookalikeSpec.put("country", "TR");
		String str = new Gson().toJson(lookalikeSpec);
		CustomAudience ca = adAccount.createCustomAudience().setName("LAL_AUTO_TEST")
				.setSubtype(CustomAudience.EnumSubtype.VALUE_LOOKALIKE).setLookalikeSpec(str).setOriginAudienceId(caid)
				.setDescription("People who purchased on my website").execute();
		System.out.println(ca);
		return ca;
	}

	public static CustomAudience createValueBaseCustomAudience(APIContext context, String accountId)
			throws APIException {
		AdAccount adAccount = new AdAccount(accountId, context);
		CustomAudience ca = adAccount.createCustomAudience().setName("VBCA_AUTO_TEST")
				.setSubtype(CustomAudience.EnumSubtype.VALUE_CUSTOM)
				.setDescription("People who purchased on my website").setIsValueBased(true)
				.setCustomerFileSource(CustomAudience.EnumCustomerFileSource.VALUE_USER_PROVIDED_ONLY).execute();
		System.out.println(ca);
		return ca;
	}

	public static void createValueBaseCustomAudienceUser(APIContext context, String caid)
			throws APIException, IOException {
		Map<String, Object> payload = Maps.newHashMap();
		List<String> schema = Lists.newArrayList();
		schema.add("MADID");
		schema.add("LOOKALIKE_VALUE");
		payload.put("schema", schema);
		List<List<? extends Object>> data = TLStream.readLines("/Users/kuojian21/Downloads/upload_count.csv").stream()
				.map(p -> Lists.newArrayList(p.split(",")[0], Float.parseFloat(p.split(",")[1])))
				.collect(Collectors.toList());
		payload.put("data", data);
		System.out.println(CustomAudience.fetchById(caid, context).createUser().setPayload(payload).execute());
	}

	public static void createCustomAudienceUser(APIContext context, String caid) throws APIException, IOException {
		Map<String, Object> payload = Maps.newHashMap();
		payload.put("schema", "MOBILE_ADVERTISER_ID");
		List<String> data = TLStream.readLines("/Users/kuojian21/Downloads/upload_count.csv").stream()
				.map(p -> p.split(",")[0])/* .limit() .map(p -> sha256(p)) */.collect(Collectors.toList());
		payload.put("data", data);
		System.out.println(CustomAudience.fetchById(caid, context).createUser().setPayload(payload).execute());
	}

	public static void share(APIContext context, String caid, List<String> accountIds) throws APIException {
		System.out
				.println(CustomAudience.fetchById(caid, context).createAdAccount().setAdaccounts(accountIds).execute());
	}

	public static void createLookalike(APIContext context, String accountId, String originAudienceId)
			throws APIException {
		AdAccount adAccount = new AdAccount(accountId, context);

		Map<String, Object> lookalikeSpec = Maps.newHashMap();

		CustomAudience ca = adAccount.createCustomAudience().setName("LAL_AUTO_TEST")
				.setSubtype(CustomAudience.EnumSubtype.VALUE_LOOKALIKE)
				.setDescription("People who purchased on my website").setOriginAudienceId(originAudienceId)
				.setLookalikeSpec(new Gson().toJson(lookalikeSpec)).execute();
		System.out.println(ca);
	}

	public static void createLookalike2(APIContext context, String accountId, String originAudienceId)
			throws APIException {
		AdAccount adAccount = new AdAccount(accountId, context);

		Map<String, Object> lookalikeSpec = Maps.newHashMap();

		CustomAudience ca = adAccount.createCustomAudience().setName("LAL_AUTO_TEST")
				.setSubtype(CustomAudience.EnumSubtype.VALUE_LOOKALIKE)
				.setDescription("People who purchased on my website").setOriginAudienceId(originAudienceId)
				.setLookalikeSpec(new Gson().toJson(lookalikeSpec)).execute();
		System.out.println(ca);
	}

	public static void main(String[] args)
			throws APIException, NoSuchMethodException, SecurityException, IllegalAccessException,
			IllegalArgumentException, InvocationTargetException, InterruptedException, ExecutionException {
		if (args.length < 1) {
			return;
		}
		/*
		 * -DsocksProxyHost= -DsocksProxyPort=8088
		 */

		// System.setProperty("socksProxyHost", "127.0.0.1");
		// System.setProperty("socksProxyPort", "8088");
		// APIContext context = new APIContext(args[0]);

	}

}
