package unkown.jar.client.render;

import net.fabricmc.fabric.api.client.rendering.v1.EntityModelLayerRegistry;
import net.fabricmc.fabric.api.client.rendering.v1.EntityRendererRegistry;
import net.minecraft.client.model.HumanoidModel;
import net.minecraft.client.model.geom.ModelLayerLocation;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.model.geom.PartPose;
import net.minecraft.client.model.geom.builders.CubeListBuilder;
import net.minecraft.client.model.geom.builders.LayerDefinition;
import net.minecraft.client.model.geom.builders.MeshDefinition;
import net.minecraft.client.model.geom.builders.PartDefinition;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.HumanoidMobRenderer;
import net.minecraft.client.renderer.entity.state.HumanoidRenderState;
import net.minecraft.resources.Identifier;
import net.minecraft.util.Mth;
import unkown.jar.UnkownJar;
import unkown.jar.entity.ModEntities;
import unkown.jar.entity.ZeroBitsEntity;

/** Renderer, render state and model of "0 Bits", all in one file. No red anywhere on this monster. */
public class ZeroBitsRenderer extends HumanoidMobRenderer<ZeroBitsEntity, ZeroState, ZeroModel> {
	private static final Identifier TEXTURE = UnkownJar.id("textures/entity/zero_bits.png");

	/** Called from the client initializer. */
	public static void init() {
		EntityModelLayerRegistry.registerModelLayer(ZeroModel.LAYER, ZeroModel::createBodyLayer);
		EntityRendererRegistry.register(ModEntities.ZERO, ZeroBitsRenderer::new);
	}

	public ZeroBitsRenderer(EntityRendererProvider.Context context) {
		super(context, new ZeroModel(context.bakeLayer(ZeroModel.LAYER)), 0.9F);
	}

	@Override
	public ZeroState createRenderState() {
		return new ZeroState();
	}

	@Override
	public void extractRenderState(ZeroBitsEntity entity, ZeroState state, float partialTick) {
		super.extractRenderState(entity, state, partialTick);
		state.swingTicks = entity.getSwingTicks();
		state.aggro = entity.isAggro();
		state.lightCoords = 0xF000F0; // full-bright so the white zero glows in the dark
	}

	@Override
	public Identifier getTextureLocation(ZeroState state) {
		return TEXTURE;
	}
}

class ZeroState extends HumanoidRenderState {
	public int swingTicks;
	public boolean aggro;
}

/**
 * 0 Bits after the concept sheet: about 4.5 blocks tall, hunched, a cube head with a glowing slashed zero,
 * a jaw that only opens while it hunts, very long arms with ten long claws, long legs with heavy boots,
 * sixteen cables/tentacles from head and back, and floating glitch shards.
 */
class ZeroModel extends HumanoidModel<ZeroState> {
	static final ModelLayerLocation LAYER = new ModelLayerLocation(UnkownJar.id("zero_bits"), "main");

	private static final int HEAD_TENT = 8;
	private static final int TENT = 16;
	private static final int SEG = 5;
	private static final int FING = 5;
	private static final int SHARDS = 8;
	private static final int SWING_DURATION = 14;
	private static final float[] LEN = {11, 11, 10, 9, 8};
	private static final float[] WIDTH = {1.9F, 1.7F, 1.4F, 1.1F, 0.8F};

	private final ModelPart[][] tent = new ModelPart[TENT][SEG];
	private final ModelPart[] foreArm = new ModelPart[2];
	private final ModelPart[][] fingers = new ModelPart[2][FING];
	private final ModelPart[][] tips = new ModelPart[2][FING];
	private final ModelPart[] shin = new ModelPart[2];
	private final ModelPart[] boot = new ModelPart[2];
	private final ModelPart[] shards = new ModelPart[SHARDS];
	private final ModelPart jaw;

	ZeroModel(ModelPart root) {
		super(root);
		ModelPart headPart = root.getChild("head");
		ModelPart bodyPart = root.getChild("body");
		this.jaw = headPart.getChild("jaw");
		for (int i = 0; i < TENT; i++) {
			ModelPart parent = i < HEAD_TENT ? headPart.getChild("t" + i) : bodyPart.getChild("t" + i);
			for (int j = 0; j < SEG; j++) {
				ModelPart seg = parent.getChild("s" + j);
				this.tent[i][j] = seg;
				parent = seg;
			}
		}
		String[] arms = {"right_arm", "left_arm"};
		String[] legs = {"right_leg", "left_leg"};
		for (int s = 0; s < 2; s++) {
			ModelPart fore = root.getChild(arms[s]).getChild("fore");
			this.foreArm[s] = fore;
			ModelPart hand = fore.getChild("hand");
			for (int i = 0; i < FING; i++) {
				ModelPart f = hand.getChild("f" + i);
				this.fingers[s][i] = f;
				this.tips[s][i] = f.getChild("tip");
			}
			ModelPart sh = root.getChild(legs[s]).getChild("shin");
			this.shin[s] = sh;
			this.boot[s] = sh.getChild("boot");
		}
		for (int i = 0; i < SHARDS; i++) {
			this.shards[i] = root.getChild("shard" + i);
		}
	}

	private static void addTentacle(PartDefinition holder, String name, float x, float y, float z, float xRot, float zRot) {
		PartDefinition parent = holder.addOrReplaceChild(name, CubeListBuilder.create(), PartPose.offsetAndRotation(x, y, z, xRot, 0.0F, zRot));
		for (int j = 0; j < SEG; j++) {
			float w = WIDTH[j];
			parent = parent.addOrReplaceChild("s" + j,
					CubeListBuilder.create().texOffs(64, 0).addBox(-w / 2, 0.0F, -w / 2, w, LEN[j], w),
					PartPose.offset(0.0F, j == 0 ? 0.0F : LEN[j - 1], 0.0F));
		}
	}

	static LayerDefinition createBodyLayer() {
		MeshDefinition mesh = new MeshDefinition();
		PartDefinition root = mesh.getRoot();

		// ---- cube head with the glowing zero, a jaw, and cables
		PartDefinition head = root.addOrReplaceChild("head",
				CubeListBuilder.create().texOffs(0, 0).addBox(-6.0F, -12.0F, -6.0F, 12.0F, 12.0F, 12.0F)
						.texOffs(64, 0).addBox(-2.0F, 0.0F, -2.0F, 4.0F, 3.0F, 4.0F),
				PartPose.offset(0.0F, -33.0F, 0.0F));
		head.addOrReplaceChild("hat", CubeListBuilder.create(), PartPose.ZERO); // "hat" must be a child of "head"
		head.addOrReplaceChild("jaw",
				CubeListBuilder.create().texOffs(0, 26).addBox(-5.0F, 0.0F, -6.0F, 10.0F, 3.0F, 6.0F),
				PartPose.offset(0.0F, -0.4F, 0.0F));
		for (int i = 0; i < HEAD_TENT; i++) {
			float x = -5.0F + i * (10.0F / 7.0F);
			addTentacle(head, "t" + i, x, -11.0F + (i % 4) * 2.5F, 5.6F, 0.2F + (i % 2) * 0.1F, (i - 3.5F) * 0.05F);
		}

		// ---- tall torso with cables from the back
		PartDefinition body = root.addOrReplaceChild("body",
				CubeListBuilder.create().texOffs(0, 38).addBox(-4.0F, 0.0F, -2.5F, 8.0F, 18.0F, 5.0F),
				PartPose.offset(0.0F, -30.0F, 0.0F));
		for (int k = 0; k < 8; k++) {
			boolean left = k >= 4;
			int n = k % 4;
			float outward = (left ? -1.0F : 1.0F) * (0.18F + n * 0.2F);
			addTentacle(body, "t" + (HEAD_TENT + k), left ? 4.2F : -4.2F, 1.0F + n * 3.5F, 2.8F, 0.2F + n * 0.1F, outward);
		}

		// ---- very long arms with ten long claws
		for (int s = 0; s < 2; s++) {
			float x = s == 0 ? -6.4F : 6.4F;
			PartDefinition arm = root.addOrReplaceChild(s == 0 ? "right_arm" : "left_arm",
					CubeListBuilder.create().texOffs(64, 0).addBox(-1.4F, -1.0F, -1.4F, 2.8F, 19.0F, 2.8F),
					PartPose.offset(x, -28.0F, 0.0F));
			PartDefinition fore = arm.addOrReplaceChild("fore",
					CubeListBuilder.create().texOffs(64, 0).addBox(-1.2F, 0.0F, -1.2F, 2.4F, 18.0F, 2.4F),
					PartPose.offset(0.0F, 18.0F, 0.0F));
			PartDefinition hand = fore.addOrReplaceChild("hand",
					CubeListBuilder.create().texOffs(64, 0).addBox(-1.6F, 0.0F, -1.2F, 3.2F, 3.5F, 2.4F),
					PartPose.offset(0.0F, 18.0F, 0.0F));
			for (int i = 0; i < FING; i++) {
				PartDefinition finger = hand.addOrReplaceChild("f" + i,
						CubeListBuilder.create().texOffs(64, 0).addBox(-0.35F, 0.0F, -0.35F, 0.7F, 8.0F, 0.7F),
						PartPose.offsetAndRotation(-1.4F + i * 0.7F, 3.5F, 0.0F, 0.3F, 0.0F, (i - 2.0F) * 0.2F));
				finger.addOrReplaceChild("tip",
						CubeListBuilder.create().texOffs(64, 0).addBox(-0.3F, 0.0F, -0.3F, 0.6F, 5.0F, 0.6F),
						PartPose.offsetAndRotation(0.0F, 8.0F, 0.0F, 0.9F, 0.0F, 0.0F));
			}
		}

		// ---- long legs with heavy boots
		for (int s = 0; s < 2; s++) {
			float x = s == 0 ? -2.6F : 2.6F;
			PartDefinition leg = root.addOrReplaceChild(s == 0 ? "right_leg" : "left_leg",
					CubeListBuilder.create().texOffs(64, 0).addBox(-2.0F, 0.0F, -2.0F, 4.0F, 18.0F, 4.0F),
					PartPose.offset(x, -12.0F, 0.0F));
			PartDefinition sh = leg.addOrReplaceChild("shin",
					CubeListBuilder.create().texOffs(64, 0).addBox(-1.8F, 0.0F, -1.8F, 3.6F, 14.0F, 3.6F),
					PartPose.offset(0.0F, 18.0F, 0.0F));
			sh.addOrReplaceChild("boot",
					CubeListBuilder.create().texOffs(0, 64).addBox(-3.0F, 0.0F, -6.0F, 6.0F, 4.0F, 10.0F),
					PartPose.offset(0.0F, 14.0F, 0.0F));
		}

		// ---- glitch shards
		for (int i = 0; i < SHARDS; i++) {
			root.addOrReplaceChild("shard" + i,
					CubeListBuilder.create().texOffs(64, 0).addBox(-1.2F, -1.2F, -1.2F, 2.4F, 2.4F, 2.4F),
					PartPose.ZERO);
		}
		return LayerDefinition.create(mesh, 128, 128);
	}

	@Override
	public void setupAnim(ZeroState state) {
		super.setupAnim(state);
		float t = state.ageInTicks;
		boolean aggro = state.aggro;
		float spd = Mth.clamp(state.walkAnimationSpeed, 0.0F, 1.0F);
		float walk = state.walkAnimationPos;

		// ---- posture: hunched; in the aggro phase the whole body changes (deeper hunch, head thrust forward, arms flared)
		this.body.xRot = (aggro ? 0.62F : 0.38F) + 0.03F * Mth.sin(t * 0.06F);
		this.head.xRot += aggro ? 0.8F : 0.5F;
		this.head.zRot = 0.05F * Mth.sin(t * 0.05F);
		int glitch = (int) t % (aggro ? 29 : 67);
		if (glitch < 2) { // glitch twitch
			this.head.zRot = glitch == 0 ? 0.45F : -0.4F;
			this.head.yRot += glitch == 0 ? 0.55F : -0.5F;
		}

		// ---- jaw opens only while it hunts
		this.jaw.xRot = aggro ? 0.8F + 0.3F * Math.abs(Mth.sin(t * 0.9F)) : 0.0F;

		// ---- long arms hang far forward, claws twitching
		float sway = 0.04F * Mth.sin(t * 0.07F);
		float flare = aggro ? 0.32F : 0.06F;
		this.rightArm.xRot = (aggro ? 0.45F : 0.25F) + sway + Mth.cos(walk * 0.6662F + (float) Math.PI) * 0.5F * spd;
		this.leftArm.xRot = (aggro ? 0.45F : 0.25F) - sway + Mth.cos(walk * 0.6662F) * 0.5F * spd;
		this.rightArm.zRot = flare;
		this.leftArm.zRot = -flare;
		this.foreArm[0].xRot = -0.35F;
		this.foreArm[1].xRot = -0.35F;

		// ---- swing: arms rise, then slam down
		if (state.swingTicks > 0) {
			float p = 1.0F - state.swingTicks / (float) SWING_DURATION;
			if (p < 0.45F) {
				float k = p / 0.45F;
				this.rightArm.xRot = -2.7F * k;
				this.leftArm.xRot = -2.4F * k;
				this.foreArm[0].xRot = -0.9F * k;
				this.foreArm[1].xRot = -0.9F * k;
				this.body.xRot -= 0.35F * k;
				this.head.xRot -= 0.3F * k;
			} else {
				float k = (p - 0.45F) / 0.55F;
				float e = 1.0F - (1.0F - k) * (1.0F - k);
				this.rightArm.xRot = -2.7F + 3.5F * e;
				this.leftArm.xRot = -2.4F + 3.2F * e;
				this.foreArm[0].xRot = -0.9F + 1.0F * e;
				this.foreArm[1].xRot = -0.9F + 1.0F * e;
				this.body.xRot += 0.35F * e;
				this.head.xRot += 0.3F * e;
			}
		}
		float spread = aggro ? 1.5F : 1.0F;
		for (int s = 0; s < 2; s++) {
			for (int i = 0; i < FING; i++) {
				float tw = Mth.sin(t * 0.28F + i * 1.2F + s * 2.1F);
				this.fingers[s][i].xRot = 0.25F + 0.14F * tw;
				this.fingers[s][i].zRot = (i - 2.0F) * 0.2F * spread;
				this.tips[s][i].xRot = 0.9F + 0.22F * tw;
			}
		}

		// ---- legs: long strides, knees bend, heavy boots
		this.shin[0].xRot = 0.12F + Math.max(0.0F, -this.rightLeg.xRot) * 1.1F;
		this.shin[1].xRot = 0.12F + Math.max(0.0F, -this.leftLeg.xRot) * 1.1F;
		this.boot[0].xRot = -this.shin[0].xRot * 0.5F;
		this.boot[1].xRot = -this.shin[1].xRot * 0.5F;

		// ---- cables: flowing sway; streaming behind and flaring wider while it hunts
		float stream = aggro ? 0.5F : 0.0F;
		for (int i = 0; i < TENT; i++) {
			float ph = i * 0.81F;
			for (int j = 0; j < SEG; j++) {
				float amp = 0.10F + 0.07F * j + spd * 0.10F * j;
				float x = amp * Mth.sin(t * (aggro ? 0.14F : 0.08F) + ph + j * 0.7F);
				float z = amp * 0.8F * Mth.sin(t * 0.07F + ph * 1.3F + j * 0.6F);
				this.tent[i][j].xRot = (j == 0 ? 0.0F : 0.12F) + x + stream * (j == 0 ? 0.6F : 0.3F);
				this.tent[i][j].zRot = z;
			}
		}

		// ---- floating glitch shards, flickering
		float orbit = t * (aggro ? 0.05F : 0.025F);
		for (int i = 0; i < SHARDS; i++) {
			float a = orbit + i * 0.9F;
			float r = 10.0F + 7.0F * Mth.sin(t * 0.04F + i * 1.7F);
			this.shards[i].x = Mth.cos(a) * r;
			this.shards[i].z = Mth.sin(a) * r;
			this.shards[i].y = -40.0F + 48.0F * (0.5F + 0.5F * Mth.sin(t * 0.03F + i * 2.1F));
			this.shards[i].xRot = t * 0.08F + i;
			this.shards[i].yRot = t * 0.11F + i * 2.0F;
			this.shards[i].visible = ((int) (t + i * 7) % 29) > 2;
		}
	}
}
