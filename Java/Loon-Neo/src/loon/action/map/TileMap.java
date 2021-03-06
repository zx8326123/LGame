/**
 * Copyright 2008 - 2015 The Loon Game Engine Authors
 * 
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not
 * use this file except in compliance with the License. You may obtain a copy of
 * the License at
 * 
 * http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations under
 * the License.
 * 
 * @project loon
 * @author cping
 * @email：javachenpeng@yahoo.com
 * @version 0.5
 */
package loon.action.map;

import java.io.IOException;

import loon.LObject;
import loon.LSysException;
import loon.LSystem;
import loon.LTexture;
import loon.PlayerUtils;
import loon.Screen;
import loon.LTexture.Format;
import loon.action.ActionBind;
import loon.action.ActionTween;
import loon.action.map.colider.TileImpl;
import loon.action.sprite.Animation;
import loon.action.sprite.ISprite;
import loon.action.sprite.MoveControl;
import loon.action.sprite.SpriteBatch;
import loon.action.sprite.Sprites;
import loon.canvas.Image;
import loon.canvas.LColor;
import loon.event.DrawListener;
import loon.geom.Affine2f;
import loon.geom.RectBox;
import loon.geom.Vector2f;
import loon.opengl.GLEx;
import loon.opengl.LTexturePack;
import loon.opengl.LTexturePackClip;
import loon.utils.MathUtils;
import loon.utils.TArray;

/**
 * 一个简单的二维数组地图构造以及显示类.复杂地图请使用tmx包
 */
public class TileMap extends LObject<ISprite> implements ISprite {

	private LTexture _background;

	// 地图的Sprites
	private Sprites _mapSprites;

	// Screen的Sprites
	private Sprites _sprites;

	private int firstTileX;

	private int firstTileY;

	private int lastTileX;

	private int lastTileY;

	public DrawListener<TileMap> listener;

	private LTexturePack texturePack;

	private TArray<TileImpl> arrays = new TArray<TileImpl>(10);

	private TArray<Animation> animations = new TArray<Animation>();

	private final int maxWidth, maxHeight;

	private final Field2D field;

	private int lastOffsetX, lastOffsetY;

	private ActionBind follow;

	private Vector2f offset;

	private Format format;

	private boolean active, dirty;

	private boolean visible, roll;

	private boolean playAnimation;

	private LColor baseColor = LColor.white;

	private float scaleX = 1f, scaleY = 1f;

	public TileMap(String fileName, int tileWidth, int tileHeight) throws IOException {
		this(fileName, tileWidth, tileHeight, LSystem.viewSize.getWidth(), LSystem.viewSize.getHeight(), Format.LINEAR);
	}

	public TileMap(String fileName, Screen screen, int tileWidth, int tileHeight) throws IOException {
		this(fileName, screen, tileWidth, tileHeight, LSystem.viewSize.getWidth(), LSystem.viewSize.getHeight(),
				Format.LINEAR);
	}

	public TileMap(String fileName, int tileWidth, int tileHeight, int mWidth, int mHeight) throws IOException {
		this(fileName, tileWidth, tileHeight, mWidth, mHeight, Format.LINEAR);
	}

	public TileMap(String fileName, Screen screen, int tileWidth, int tileHeight, int mWidth, int mHeight)
			throws IOException {
		this(fileName, screen, tileWidth, tileHeight, mWidth, mHeight, Format.LINEAR);
	}

	public TileMap(String fileName, int tileWidth, int tileHeight, int mWidth, int mHeight, Format format)
			throws IOException {
		this(TileMapConfig.loadAthwartArray(fileName), tileWidth, tileHeight, mWidth, mHeight, format);
	}

	public TileMap(String fileName, Screen screen, int tileWidth, int tileHeight, int mWidth, int mHeight,
			Format format) throws IOException {
		this(TileMapConfig.loadAthwartArray(fileName), screen, tileWidth, tileHeight, mWidth, mHeight, format);
	}

	public TileMap(int[][] maps, int tileWidth, int tileHeight, int mWidth, int mHeight, Format format) {
		this(new Field2D(maps, tileWidth, tileHeight), mWidth, mHeight, format);
	}

	public TileMap(int[][] maps, Screen screen, int tileWidth, int tileHeight, int mWidth, int mHeight, Format format) {
		this(new Field2D(maps, tileWidth, tileHeight), screen, mWidth, mHeight, format);
	}

	public TileMap(int[][] maps, int tileWidth, int tileHeight, int mWidth, int mHeight) {
		this(maps, tileWidth, tileHeight, mWidth, mHeight, Format.LINEAR);
	}

	public TileMap(int[][] maps, Screen screen, int tileWidth, int tileHeight, int mWidth, int mHeight) {
		this(maps, screen, tileWidth, tileHeight, mWidth, mHeight, Format.LINEAR);
	}

	public TileMap(int[][] maps, int tileWidth, int tileHeight) {
		this(maps, tileWidth, tileHeight, LSystem.viewSize.getWidth(), LSystem.viewSize.getHeight());
	}

	public TileMap(int[][] maps, Screen screen, int tileWidth, int tileHeight) {
		this(maps, screen, tileWidth, tileHeight, LSystem.viewSize.getWidth(), LSystem.viewSize.getHeight());
	}

	public TileMap(Field2D field) {
		this(field, LSystem.viewSize.getWidth(), LSystem.viewSize.getHeight(), Format.LINEAR);
	}

	public TileMap(Field2D field, Screen screen) {
		this(field, screen, LSystem.viewSize.getWidth(), LSystem.viewSize.getHeight(), Format.LINEAR);
	}

	public TileMap(Field2D field, Format format) {
		this(field, LSystem.viewSize.getWidth(), LSystem.viewSize.getHeight(), format);
	}

	public TileMap(Field2D field, Screen screen, Format format) {
		this(field, screen, LSystem.viewSize.getWidth(), LSystem.viewSize.getHeight(), format);
	}

	public TileMap(Field2D field, int mWidth, int mHeight, Format format) {
		this(field, null, LSystem.viewSize.getWidth(), LSystem.viewSize.getHeight(), format);
	}

	public TileMap(Field2D field, Screen screen, int mWidth, int mHeight, Format format) {
		this.field = field;
		if (field != null && mWidth == -1 && mHeight == -1) {
			this.maxWidth = field.getViewWidth();
			this.maxHeight = field.getViewHeight();
		} else {
			this.maxWidth = mWidth;
			this.maxHeight = mHeight;
		}
		if (field == null) {
			this.offset = new Vector2f(0, 0);
		} else {
			this.offset = field.getOffset();
		}
		this.texturePack = new LTexturePack();
		this.format = format;
		this.lastOffsetX = -1;
		this.lastOffsetY = -1;
		this.active = true;
		this.dirty = true;
		this.visible = true;
		this._mapSprites = new Sprites("TileMapSprites", screen == null ? LSystem.getProcess().getScreen() : screen,
				maxWidth, maxHeight);
		this.texturePack.setFormat(format);
	}

	public static TileMap loadCharsMap(String resName, int tileWidth, int tileHeight) {
		return new TileMap(TileMapConfig.loadCharsField(resName, tileWidth, tileHeight));
	}

	public TileMap setImagePackAuto(String fileName, int tileWidth, int tileHeight) {
		if (texturePack != null) {
			texturePack.close();
			texturePack = null;
		}
		texturePack = new LTexturePack(fileName, LTexturePackClip.getTextureSplit(fileName, tileWidth, tileHeight));
		texturePack.packed(format);
		return this;
	}

	public TileMap setImagePack(String fileName, LTexturePackClip[] clips) {
		return setImagePack(fileName, new TArray<LTexturePackClip>(clips));
	}

	public TileMap setImagePack(String fileName, TArray<LTexturePackClip> clips) {
		if (texturePack != null) {
			texturePack.close();
			texturePack = null;
		}
		this.active = false;
		this.dirty = true;
		texturePack = new LTexturePack(fileName, clips);
		texturePack.packed(format);
		return this;
	}

	public TileMap setImagePack(String file) {
		if (texturePack != null) {
			texturePack.close();
			texturePack = null;
		}
		this.active = false;
		this.dirty = true;
		texturePack = new LTexturePack(file);
		texturePack.packed(format);
		return this;
	}

	public TileMap removeTile(int id) {
		for (TileImpl tile : arrays) {
			if (tile.idx == id) {
				if (tile.isAnimation) {
					animations.remove(tile.animation);
				}
				arrays.remove(tile);
			}
		}
		if (animations.size == 0) {
			playAnimation = false;
		}
		this.dirty = true;
		return this;
	}

	public int putAnimationTile(int id, Animation animation, Attribute attribute) {
		if (active) {
			TileImpl tile = new TileImpl(id);
			tile.imgId = -1;
			tile.attribute = attribute;
			if (animation != null && animation.getTotalFrames() > 0) {
				tile.isAnimation = true;
				tile.animation = animation;
				playAnimation = true;
			}
			animations.add(animation);
			arrays.add(tile);
			dirty = true;
			return tile.imgId;
		} else {
			throw new LSysException("Map is no longer active, you can not add new tiles !");
		}
	}

	public int putAnimationTile(int id, String res, int w, int h, int timer) {
		return putAnimationTile(id, Animation.getDefaultAnimation(res, w, h, timer), null);
	}

	public int putAnimationTile(int id, Animation animation) {
		return putAnimationTile(id, animation, null);
	}

	public int putTile(int id, Image img, Attribute attribute) {
		if (active) {
			TileImpl tile = new TileImpl(id);
			tile.imgId = texturePack.putImage(img);
			tile.attribute = attribute;
			arrays.add(tile);
			dirty = true;
			return tile.imgId;
		} else {
			throw new LSysException("Map is no longer active, you can not add new tiles !");
		}
	}

	public int putTile(int id, Image img) {
		return putTile(id, img, null);
	}

	public int putTile(int id, LTexture img, Attribute attribute) {
		if (active) {
			TileImpl tile = new TileImpl(id);
			tile.imgId = texturePack.putImage(img);
			tile.attribute = attribute;
			arrays.add(tile);
			dirty = true;
			return tile.imgId;
		} else {
			throw new LSysException("Map is no longer active, you can not add new tiles !");
		}
	}

	public int putTile(int id, LTexture img) {
		return putTile(id, img, null);
	}

	public int putTile(int id, String res, Attribute attribute) {
		if (active) {
			TileImpl tile = new TileImpl(id);
			tile.imgId = texturePack.putImage(res);
			tile.attribute = attribute;
			arrays.add(tile);
			dirty = true;
			return tile.imgId;
		} else {
			throw new LSysException("Map is no longer active, you can not add new tiles !");
		}
	}

	public int putTile(int id, String res) {
		return putTile(id, res, null);
	}

	public TileMap putTile(int id, int imgId, Attribute attribute) {
		if (active) {
			TileImpl tile = new TileImpl(id);
			tile.imgId = imgId;
			tile.attribute = attribute;
			arrays.add(tile);
			dirty = true;
		} else {
			new LSysException("Map is no longer active, you can not add new tiles !");
		}
		return this;
	}

	public TileMap putTile(int id, int imgId) {
		return putTile(id, imgId, null);
	}

	public TileImpl getTile(int id) {
		for (TileImpl tile : arrays) {
			if (tile.idx == id) {
				return tile;
			}
		}
		return null;
	}

	public int[][] getMap() {
		return field.getMap();
	}

	public boolean isActive() {
		return active;
	}

	public TileMap pack() {
		completed();
		return this;
	}

	public TileMap completed() {
		if (texturePack != null) {
			if (!texturePack.isPacked()) {
				texturePack.packed(format);
			}
			int[] list = texturePack.getIdList();
			active = true;
			dirty = true;
			for (int i = 0, size = list.length; i < size; i++) {
				int id = list[i];
				putTile(id, id);
			}
		}
		return this;
	}

	public Format getFormat() {
		return format;
	}

	public int getTileID(int x, int y) {
		if (x >= 0 && x < field.getWidth() && y >= 0 && y < field.getHeight()) {
			return field.getTileType(x, y);
		} else {
			return -1;
		}
	}

	public TileMap setTileID(int x, int y, int id) {
		if (x >= 0 && x < field.getWidth() && y >= 0 && y < field.getHeight()) {
			field.setTileType(x, y, id);
		}
		return this;
	}

	public TileMap addMapSprite(ISprite sprite) {
		_mapSprites.add(sprite);
		return this;
	}

	public TileMap addMapSpriteAt(ISprite sprite, float x, float y) {
		_mapSprites.addAt(sprite, x, y);
		return this;
	}

	public TileMap removeMapSprite(int idx) {
		_mapSprites.remove(idx);
		return this;
	}

	public TileMap removeMapSprite(ISprite sprite) {
		_mapSprites.remove(sprite);
		return this;
	}

	public TileMap removeMapSprite(int start, int end) {
		_mapSprites.remove(start, end);
		return this;
	}

	public void draw(GLEx g) {
		if (this.roll) {
			this.offset = this.toRollPosition(this.offset);
		}
		draw(g, null, x() + offset.x(), y() + offset.y());
	}

	public void draw(GLEx g, SpriteBatch batch, int offsetX, int offsetY) {
		final boolean useBatch = (batch != null);
		if (useBatch) {
			if (_background != null) {
				batch.draw(_background, offsetX, offsetY);
			}
		} else {
			if (_background != null) {
				g.draw(_background, offsetX, offsetY);
			}
		}
		if (!active || texturePack == null) {
			completed();
			return;
		}
		dirty = dirty || !texturePack.existCache();
		if (!dirty && lastOffsetX == offsetX && lastOffsetY == offsetY) {
			texturePack.postCache();
			if (playAnimation) {
				int[][] maps = field.getMap();
				for (int i = firstTileX; i < lastTileX; i++) {
					for (int j = firstTileY; j < lastTileY; j++) {
						if (i > -1 && j > -1 && i < field.getWidth() && j < field.getHeight()) {
							int id = maps[j][i];
							for (TileImpl tile : arrays) {
								if (tile.isAnimation && tile.idx == id) {
									if (useBatch) {
										LColor tmp = batch.getColor();
										batch.setColor(baseColor);
										batch.draw(tile.animation.getSpriteImage(),
												field.tilesToWidthPixels(i) + offsetX,
												field.tilesToHeightPixels(j) + offsetY, field.getTileWidth(),
												field.getTileHeight());
										batch.setColor(tmp);
									} else {
										g.draw(tile.animation.getSpriteImage(), field.tilesToWidthPixels(i) + offsetX,
												field.tilesToHeightPixels(j) + offsetY, field.getTileWidth(),
												field.getTileHeight(), baseColor);
									}
								}
							}
						}
					}
				}
			}
		} else {
			if (arrays.size == 0) {
				throw new LSysException("Not to add any tiles !");
			}

			texturePack.glBegin();

			firstTileX = field.pixelsToTilesWidth(-offsetX);
			firstTileY = field.pixelsToTilesHeight(-offsetY);

			lastTileX = firstTileX + field.pixelsToTilesWidth(maxWidth) + 1;
			lastTileX = MathUtils.min(lastTileX, field.getWidth());
			lastTileY = firstTileY + field.pixelsToTilesHeight(maxHeight) + 1;
			lastTileY = MathUtils.min(lastTileY, field.getHeight());
			int[][] maps = field.getMap();
			for (int i = firstTileX; i < lastTileX; i++) {
				for (int j = firstTileY; j < lastTileY; j++) {
					if (i > -1 && j > -1 && i < field.getWidth() && j < field.getHeight()) {
						int id = maps[j][i];
						for (TileImpl tile : arrays) {
							if (playAnimation) {
								if (tile.idx == id) {
									if (tile.isAnimation) {
										if (useBatch) {
											LColor tmp = batch.getColor();
											batch.setColor(baseColor);
											batch.draw(tile.animation.getSpriteImage(),
													field.tilesToWidthPixels(i) + offsetX,
													field.tilesToHeightPixels(j) + offsetY, field.getTileWidth(),
													field.getTileHeight());
											batch.setColor(tmp);
										} else {
											g.draw(tile.animation.getSpriteImage(),
													field.tilesToWidthPixels(i) + offsetX,
													field.tilesToHeightPixels(j) + offsetY, field.getTileWidth(),
													field.getTileHeight(), baseColor);
										}
									} else {
										texturePack.draw(tile.imgId, field.tilesToWidthPixels(i) + offsetX,
												field.tilesToHeightPixels(j) + offsetY, field.getTileWidth(),
												field.getTileHeight(), baseColor);
									}
								}
							} else if (tile.idx == id) {
								texturePack.draw(tile.imgId, field.tilesToWidthPixels(i) + offsetX,
										field.tilesToHeightPixels(j) + offsetY, field.getTileWidth(),
										field.getTileHeight(), baseColor);
							}

						}
					}
				}
			}
			texturePack.glEnd();
			texturePack.saveCache();
			lastOffsetX = offsetX;
			lastOffsetY = offsetY;
			dirty = false;
		}

		if (listener != null) {
			listener.draw(g, offsetX, offsetY);
		}
	}

	public void scrollDown(float distance) {
		this.offset.y = limitOffsetY(MathUtils.min((this.offset.y + distance),
				(MathUtils.max(0, this.field.getViewHeight() - getContainerHeight()))));
	}

	public void scrollLeft(float distance) {
		this.offset.x = limitOffsetX(MathUtils.max(this.offset.x - distance, 0));
	}

	public void scrollLeftUp(float distance) {
		this.scrollUp(distance);
		this.scrollLeft(distance);
	}

	public void scrollRight(float distance) {
		this.offset.x = limitOffsetX(MathUtils.min((this.offset.x + distance),
				(MathUtils.max(0, this.field.getViewWidth() - getContainerWidth()))));
	}

	public void scrollUp(float distance) {
		this.offset.y = limitOffsetY(MathUtils.max(this.offset.y - distance, 0));
	}

	public void scrollRightDown(float distance) {
		this.scrollDown(distance);
		this.scrollRight(distance);
	}

	public void scrollClear() {
		this.offset.set(0, 0);
	}

	public int[] getLimit() {
		return field.getLimit();
	}

	public TileMap setLimit(int[] limit) {
		field.setLimit(limit);
		return this;
	}

	public boolean isHit(int px, int py) {
		return field.isHit(px, py);
	}

	public boolean isHit(Vector2f v) {
		return isHit(v.x(), v.y());
	}

	public boolean isPixelHit(int px, int py) {
		return isPixelHit(px, py, 0, 0);
	}

	public boolean isPixelHit(int px, int py, int movePx, int movePy) {
		return isHit(field.pixelsToTilesWidth(field.offsetXPixel(px)) + movePx,
				field.pixelsToTilesHeight(field.offsetYPixel(py)) + movePy);
	}

	public boolean isPixelTUp(int px, int py) {
		return isPixelHit(px, py, 0, -1);
	}

	public boolean isPixelTRight(int px, int py) {
		return isPixelHit(px, py, 1, 0);
	}

	public boolean isPixelTLeft(int px, int py) {
		return isPixelHit(px, py, -1, 0);
	}

	public boolean isPixelTDown(int px, int py) {
		return isPixelHit(px, py, 0, 1);
	}

	public Vector2f getTileCollision(LObject<?> o, float newX, float newY) {
		newX = MathUtils.ceil(newX);
		newY = MathUtils.ceil(newY);

		float fromX = MathUtils.min(o.getX(), newX);
		float fromY = MathUtils.min(o.getY(), newY);
		float toX = MathUtils.max(o.getX(), newX);
		float toY = MathUtils.max(o.getY(), newY);

		int fromTileX = field.pixelsToTilesWidth(fromX);
		int fromTileY = field.pixelsToTilesHeight(fromY);
		int toTileX = field.pixelsToTilesWidth(toX + o.getWidth() - 1f);
		int toTileY = field.pixelsToTilesHeight(toY + o.getHeight() - 1f);

		for (int x = fromTileX; x <= toTileX; x++) {
			for (int y = fromTileY; y <= toTileY; y++) {
				if ((x < 0) || (x >= field.getWidth())) {
					return new Vector2f(x, y);
				}
				if ((y < 0) || (y >= field.getHeight())) {
					return new Vector2f(x, y);
				}
				if (!this.isHit(x, y)) {
					return new Vector2f(x, y);
				}
			}
		}

		return null;
	}

	public int getTileIDFromPixels(Vector2f v) {
		return getTileIDFromPixels(v.x, v.y);
	}

	public int getTileIDFromPixels(float sx, float sy) {
		float x = (sx + offset.getX());
		float y = (sy + offset.getY());
		Vector2f tileCoordinates = pixelsToTiles(x, y);
		return getTileID(MathUtils.round(tileCoordinates.getX()), MathUtils.round(tileCoordinates.getY()));
	}

	public Vector2f pixelsToTiles(float x, float y) {
		float xprime = x / field.getTileWidth() - 1;
		float yprime = y / field.getTileHeight() - 1;
		return new Vector2f(xprime, yprime);
	}

	public Field2D getField() {
		return field;
	}

	public int tilesToPixelsX(float x) {
		return field.tilesToWidthPixels(x);
	}

	public int tilesToPixelsY(float y) {
		return field.tilesToHeightPixels(y);
	}

	public int pixelsToTilesWidth(float x) {
		return field.pixelsToTilesWidth(x);
	}

	public int pixelsToTilesHeight(float y) {
		return field.pixelsToTilesHeight(y);
	}

	/**
	 * 转换坐标为像素坐标
	 * 
	 * @param x
	 * @param y
	 * @return
	 */
	public Vector2f tilesToPixels(float x, float y) {
		float xprime = x * field.getTileWidth() - offset.getX();
		float yprime = y * field.getTileHeight() - offset.getY();
		return new Vector2f(xprime, yprime);
	}

	/**
	 * 设定偏移量
	 * 
	 * @param x
	 * @param y
	 */
	public TileMap setOffset(float x, float y) {
		this.offset.set(x, y);
		return this;
	}

	/**
	 * 设定偏移量
	 * 
	 * @param offset
	 */
	public TileMap setOffset(Vector2f offset) {
		this.offset.set(offset);
		return this;
	}

	/**
	 * 获得瓦片位置
	 * 
	 * @return
	 */
	public Vector2f getOffset() {
		return offset;
	}

	public float getOffsetX() {
		return offset.x;
	}

	public float getOffsetY() {
		return offset.y;
	}

	public int getTileWidth() {
		return field.getTileWidth();
	}

	public int getTileHeight() {
		return field.getTileHeight();
	}

	@Override
	public float getHeight() {
		return field.getHeight() * field.getTileWidth();
	}

	@Override
	public float getWidth() {
		return field.getWidth() * field.getTileHeight();
	}

	public int getRow() {
		return field.getWidth();
	}

	public int getCol() {
		return field.getHeight();
	}

	public TileMap setMapValues(int v) {
		field.setValues(v);
		return this;
	}

	public Field2D getNewField2D() {
		return new Field2D(field);
	}

	public DrawListener<TileMap> getListener() {
		return listener;
	}

	public TileMap setListener(DrawListener<TileMap> l) {
		this.listener = l;
		return this;
	}

	public boolean isDirty() {
		return dirty;
	}

	public TileMap setDirty(boolean dirty) {
		this.dirty = dirty;
		return this;
	}

	@Override
	public void setVisible(boolean v) {
		this.visible = v;
	}

	@Override
	public boolean isVisible() {
		return visible;
	}

	@Override
	public void createUI(GLEx g, float offsetX, float offsetY) {
		if (!visible) {
			return;
		}
		boolean update = (_rotation != 0) || !(scaleX == 1f && scaleY == 1f);
		int blend = g.getBlendMode();
		int tmp = g.color();
		try {
			g.setBlendMode(_blend);
			g.setAlpha(_alpha);
			if (this.roll) {
				this.offset = toRollPosition(this.offset);
			}
			float newX = this._location.x + offsetX + offset.getX();
			float newY = this._location.y + offsetY + offset.getY();
			if (update) {
				g.saveTx();
				Affine2f tx = g.tx();
				if (_rotation != 0) {
					final float rotationCenterX = newX + getWidth() / 2f;
					final float rotationCenterY = newY + getHeight() / 2f;
					tx.translate(rotationCenterX, rotationCenterY);
					tx.preRotate(_rotation);
					tx.translate(-rotationCenterX, -rotationCenterY);
				}
				if ((scaleX != 1) || (scaleY != 1)) {
					final float scaleCenterX = newX + getWidth() / 2f;
					final float scaleCenterY = newY + getHeight() / 2f;
					tx.translate(scaleCenterX, scaleCenterY);
					tx.preScale(scaleX, scaleY);
					tx.translate(-scaleCenterX, -scaleCenterY);
				}
			}
			followActionObject();
			int moveX = (int) newX;
			int moveY = (int) newY;
			draw(g, null, moveX, moveY);
			if (_mapSprites != null) {
				_mapSprites.paintPos(g, moveX, moveY);
			}
		} catch (Throwable ex) {
			LSystem.error("Array2D TileMap error !", ex);
		} finally {
			if (update) {
				g.restoreTx();
			}
			g.setBlendMode(blend);
			g.setColor(tmp);
		}
	}

	@Override
	public void createUI(GLEx g) {
		createUI(g, 0, 0);
	}

	@Override
	public RectBox getCollisionBox() {
		return getRect(x() + offset.x, y() + offset.y, field.getTileWidth() * field.getWidth(),
				field.getTileHeight() * field.getHeight());
	}

	@Override
	public LTexture getBitmap() {
		return texturePack.getTexture();
	}

	@Override
	public void update(long elapsedTime) {
		if (playAnimation && animations.size > 0) {
			for (Animation a : animations) {
				a.update(elapsedTime);
			}
		}
		if (_mapSprites != null) {
			_mapSprites.update(elapsedTime);
		}
		if (listener != null) {
			listener.update(elapsedTime);
		}
	}

	public TileMap startAnimation() {
		playAnimation = true;
		return this;
	}

	public TileMap stopAnimation() {
		playAnimation = false;
		return this;
	}

	protected float limitOffsetX(float newOffsetX) {
		float offsetX = getContainerWidth() / 2 - newOffsetX;
		offsetX = MathUtils.min(offsetX, 0);
		offsetX = MathUtils.max(offsetX, getContainerWidth() - getWidth());
		return offsetX;
	}

	protected float limitOffsetY(float newOffsetY) {
		float offsetY = getContainerHeight() / 2 - newOffsetY;
		offsetY = MathUtils.min(offsetY, 0);
		offsetY = MathUtils.max(offsetY, getContainerHeight() - getHeight());
		return offsetY;
	}

	public TileMap followActionObject() {
		if (follow != null) {
			float offsetX = limitOffsetX(follow.getX());
			float offsetY = limitOffsetY(follow.getY());
			setOffset(offsetX, offsetY);
			field.setOffset(offset);
		}
		return this;
	}

	@Override
	public LColor getColor() {
		return new LColor(baseColor);
	}

	@Override
	public void setColor(LColor c) {
		if (c != null && !c.equals(baseColor)) {
			this.baseColor = c;
			this.dirty = true;
		}
	}

	public int getPixelsAtFieldType(Vector2f pos) {
		return field.getPixelsAtFieldType(pos.x, pos.y);
	}

	public int getPixelsAtFieldType(float x, float y) {
		int itsX = pixelsToTilesWidth(x);
		int itsY = pixelsToTilesHeight(y);
		return field.getPixelsAtFieldType(itsX, itsY);
	}

	@Override
	public Field2D getField2D() {
		return field;
	}

	@Override
	public float getScaleX() {
		return scaleX;
	}

	@Override
	public float getScaleY() {
		return scaleY;
	}

	@Override
	public void setScale(float sx, float sy) {
		this.scaleX = sx;
		this.scaleY = sy;
	}

	@Override
	public boolean isBounded() {
		return false;
	}

	@Override
	public boolean isContainer() {
		return true;
	}

	@Override
	public boolean inContains(float x, float y, float w, float h) {
		return field.getRect().contains(x, y, w, h);
	}

	@Override
	public RectBox getRectBox() {
		return field.getRect();
	}

	public ActionBind getFollow() {
		return follow;
	}

	public TileMap setFollow(ActionBind follow) {
		this.follow = follow;
		return this;
	}

	public TileMap followAction(ActionBind follow) {
		return setFollow(follow);
	}

	public float offsetXPixel(float x) {
		return MathUtils.iceil((x - offset.x - _location.x) / scaleX);
	}

	public float offsetYPixel(float y) {
		return MathUtils.iceil((y - offset.y - _location.y) / scaleY);
	}

	public boolean inMap(int x, int y) {
		return ((((x >= 0) && (x < maxWidth)) && (y >= 0)) && (y < maxHeight));
	}

	public MoveControl followControl(ActionBind bind) {
		followAction(bind);
		return new MoveControl(bind, this.field);
	}

	public Vector2f toRollPosition(Vector2f pos) {
		pos.x = pos.x % ((float) (field.getViewWidth()));
		pos.y = pos.y % ((float) (field.getViewHeight()));
		if (pos.x < 0f) {
			pos.x += field.getViewWidth();
		}
		if (pos.x < 0f) {
			pos.y += field.getViewHeight();
		}
		return pos;
	}

	public boolean isRoll() {
		return roll;
	}

	public TileMap setRoll(boolean roll) {
		this.roll = roll;
		return this;
	}

	public LTexture getBackground() {
		return this._background;
	}

	public TileMap setBackground(LTexture bg) {
		this._background = bg;
		return this;
	}

	@Override
	public ActionTween selfAction() {
		return PlayerUtils.set(this);
	}

	@Override
	public boolean isActionCompleted() {
		return PlayerUtils.isActionCompleted(this);
	}

	public Sprites getMapSprites() {
		return _mapSprites;
	}

	public TileMap setMapSprites(Sprites s) {
		_mapSprites = s;
		return this;
	}

	@Override
	public void setSprites(Sprites ss) {
		if (this._sprites == ss) {
			return;
		}
		this._sprites = ss;
	}

	@Override
	public Sprites getSprites() {
		return this._sprites;
	}

	@Override
	public Screen getScreen() {
		if (this._sprites == null) {
			return LSystem.getProcess().getScreen();
		}
		return this._sprites.getScreen() == null ? LSystem.getProcess().getScreen() : this._sprites.getScreen();
	}

	public float getScreenX() {
		float x = 0;
		ISprite parent = _super;
		if (parent != null) {
			x += parent.getX();
			for (; (parent = parent.getParent()) != null;) {
				x += parent.getX();
			}
		}
		return x + getX();
	}

	public float getScreenY() {
		float y = 0;
		ISprite parent = _super;
		if (parent != null) {
			y += parent.getY();
			for (; (parent = parent.getParent()) != null;) {
				y += parent.getY();
			}
		}
		return y + getY();
	}

	@Override
	public float getContainerX() {
		if (_super != null) {
			return getScreenX() - getX();
		}
		return this._sprites == null ? super.getContainerX() : this._sprites.getX();
	}

	@Override
	public float getContainerY() {
		if (_super != null) {
			return getScreenY() - getY();
		}
		return this._sprites == null ? super.getContainerY() : this._sprites.getY();
	}

	@Override
	public float getContainerWidth() {
		return this._sprites == null ? super.getContainerWidth() : this._sprites.getWidth();
	}

	@Override
	public float getContainerHeight() {
		return this._sprites == null ? super.getContainerHeight() : this._sprites.getHeight();
	}

	public boolean isClosed() {
		return isDisposed();
	}

	@Override
	public String toString() {
		return field.toString();
	}

	@Override
	public void close() {
		visible = false;
		playAnimation = false;
		roll = false;
		animations.clear();
		if (texturePack != null) {
			texturePack.close();
			texturePack = null;
		}
		if (_mapSprites != null) {
			_mapSprites.close();
			_mapSprites = null;
		}
		if (_background != null) {
			_background.close();
			_background = null;
		}
		removeActionEvents(this);
		setState(State.DISPOSED);
	}

}
