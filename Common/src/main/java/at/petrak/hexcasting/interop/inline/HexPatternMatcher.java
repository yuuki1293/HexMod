package at.petrak.hexcasting.interop.inline;

import at.petrak.hexcasting.api.HexAPI;
import at.petrak.hexcasting.api.casting.math.HexDir;
import at.petrak.hexcasting.api.casting.math.HexPattern;
import com.samsthenerd.inline.api.client.InlineMatch;
import com.samsthenerd.inline.api.client.MatcherInfo;
import com.samsthenerd.inline.api.client.matchers.RegexMatcher;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Tuple;
import org.jetbrains.annotations.NotNull;

import javax.annotation.Nullable;
import java.util.HashMap;
import java.util.Map;
import java.util.regex.MatchResult;
import java.util.regex.Pattern;

public class HexPatternMatcher implements RegexMatcher {

    private static final ResourceLocation patternMatcherID = HexAPI.modLoc("pattern");
    private static final MatcherInfo patternMatcherInfo = MatcherInfo.fromId(patternMatcherID);

    // thx kyra <3
    private static final Pattern PATTERN_PATTERN_REGEX = Pattern.compile("(?<escaped>\\\\?)(?:HexPattern)?[<(\\[{]\\s*(?<direction>[a-z_-]+)(?:\\s*[, ]\\s*(?<pattern>[aqweds]+))?\\s*[>)\\]}]", Pattern.CASE_INSENSITIVE);

    public static HexPatternMatcher INSTANCE = new HexPatternMatcher();

    public Pattern getRegex(){
        return PATTERN_PATTERN_REGEX;
    }

    @NotNull
    public Tuple<InlineMatch, Integer> getMatchAndGroup(MatchResult regexMatch) {
        String escaped = regexMatch.group(1);
        String dirString = regexMatch.group(2).toLowerCase().strip().replace("_", "");
        String angleSigs = regexMatch.group(3);
        if(escaped == null){
            return new Tuple<>(new InlineMatch.TextMatch(Component.literal("")), 1);
        }
        // need to convert dirString to a HexDir
        HexDir dir = dirMap.get(dirString);
        if(dir == null)
            return new Tuple<>(null, 0);
        HexPattern pat;
        try{
            pat = HexPattern.fromAngles(angleSigs, dir);
            InlinePatternData patData = new InlinePatternData(pat);
            return new Tuple<>(new InlineMatch.DataMatch(patData, patData.getExtraStyle()), 0);
        } catch (Exception e){
            return new Tuple<>(null, 0);
        }
    }

    // not really used since we're doing escaping
    @Nullable
    public InlineMatch getMatch(MatchResult mr){
        return null; // nop
    }

    public MatcherInfo getInfo(){
        return patternMatcherInfo;
    }

    /**
     * Get the ID for this matcher
     * @return matcher's ID
     */
    public ResourceLocation getId(){
        return patternMatcherID;
    }

    private static final Map<String, HexDir> dirMap = new HashMap<>();

    static {
        dirMap.put("northwest", HexDir.NORTH_WEST);
        dirMap.put("west", HexDir.WEST);
        dirMap.put("southwest", HexDir.SOUTH_WEST);
        dirMap.put("southeast", HexDir.SOUTH_EAST);
        dirMap.put("east", HexDir.EAST);
        dirMap.put("northeast", HexDir.NORTH_EAST);
        dirMap.put("nw", HexDir.NORTH_WEST);

        dirMap.put("w", HexDir.WEST);
        dirMap.put("sw", HexDir.SOUTH_WEST);
        dirMap.put("se", HexDir.SOUTH_EAST);
        dirMap.put("e", HexDir.EAST);
        dirMap.put("ne", HexDir.NORTH_EAST);
    }
}